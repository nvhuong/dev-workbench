package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps collections added by another process (for example the MCP server) when the desktop app saves its in-memory
 * tree over a collections.json that changed on disk in the meantime.
 *
 * <p>Three-way rule for each group or request found on disk but missing in memory: if it was part of the last
 * document the app loaded or saved, the user deleted it in the app, so it stays deleted; otherwise it was added
 * externally and is copied into memory under the same parent (or as a new top-level collection).
 */
public final class CollectionExternalMerge {
    private CollectionExternalMerge() {
    }

    public static Set<String> ids(CollectionDocument document) {
        Set<String> ids = new HashSet<>();
        if (document != null) {
            for (CollectionNode root : document.getRoots()) collect(root, ids);
        }
        return ids;
    }

    /** The merged document and how many externally added nodes it gained. */
    public record Result(CollectionDocument document, int added) {
    }

    public static Result keepExternalAdditions(CollectionDocument memory, CollectionDocument disk, Set<String> baseIds) {
        if (memory == null || disk == null) return new Result(memory, 0);
        Map<String, CollectionNode> memoryGroups = new HashMap<>();
        Set<String> memoryIds = new HashSet<>();
        for (CollectionNode root : memory.getRoots()) index(root, memoryGroups, memoryIds);
        Set<String> base = baseIds == null ? Set.of() : baseIds;
        int[] added = {0};
        List<CollectionNode> newRoots = new ArrayList<>();
        for (CollectionNode diskRoot : disk.getRoots()) {
            String id = id(diskRoot);
            if (id.isEmpty()) continue;
            if (!memoryIds.contains(id)) {
                if (!base.contains(id)) {
                    newRoots.add(diskRoot);
                    added[0]++;
                }
                continue;
            }
            mergeChildren(diskRoot, memoryGroups, memoryIds, base, added);
        }
        if (newRoots.isEmpty()) {
            return new Result(memory, added[0]);
        }
        List<CollectionNode> roots = memory.mutableRootsCopy();
        roots.addAll(newRoots);
        return new Result(new CollectionDocument(roots), added[0]);
    }

    private static void mergeChildren(CollectionNode diskGroup, Map<String, CollectionNode> memoryGroups,
                                      Set<String> memoryIds, Set<String> base, int[] added) {
        CollectionNode memoryGroup = memoryGroups.get(id(diskGroup));
        if (memoryGroup == null) return;
        for (CollectionNode child : diskGroup.getChildren()) {
            String id = id(child);
            if (id.isEmpty()) continue;
            if (!memoryIds.contains(id)) {
                if (!base.contains(id)) {
                    memoryGroup.addChild(child);
                    added[0]++;
                }
            } else if (child.isGroup()) {
                mergeChildren(child, memoryGroups, memoryIds, base, added);
            }
        }
    }

    private static void index(CollectionNode node, Map<String, CollectionNode> groups, Set<String> ids) {
        String id = id(node);
        if (!id.isEmpty()) ids.add(id);
        if (node.isGroup()) {
            if (!id.isEmpty()) groups.put(id, node);
            for (CollectionNode child : node.getChildren()) index(child, groups, ids);
        }
    }

    private static void collect(CollectionNode node, Set<String> ids) {
        String id = id(node);
        if (!id.isEmpty()) ids.add(id);
        if (node.isGroup()) {
            for (CollectionNode child : node.getChildren()) collect(child, ids);
        }
    }

    private static String id(CollectionNode node) {
        if (node == null) return "";
        if (node.isGroup()) {
            RequestGroup group = node.asGroup();
            return group == null || group.getId() == null ? "" : group.getId();
        }
        HttpRequestItem request = node.getRequest();
        return request == null || request.getId() == null ? "" : request.getId();
    }
}
