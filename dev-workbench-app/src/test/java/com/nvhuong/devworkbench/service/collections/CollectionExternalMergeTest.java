package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CollectionExternalMergeTest {

    @Test
    public void keepsExternalAdditionsButNotItemsDeletedInTheApp() {
        // Base (last load): collection A with a0 and a1.
        Set<String> base = Set.of("A", "a0", "a1");
        // In the app: a0 deleted, a2 added.
        CollectionDocument memory = doc(group("A", request("a1"), request("a2")));
        // On disk (MCP): a0 still there, m1 added to A, new collection B with b1.
        CollectionDocument disk = doc(group("A", request("a0"), request("a1"), request("m1")), group("B", request("b1")));

        CollectionExternalMerge.Result result = CollectionExternalMerge.keepExternalAdditions(memory, disk, base);

        assertEquals(result.added(), 2, "m1 and collection B");
        assertEquals(CollectionExternalMerge.ids(result.document()), Set.of("A", "a1", "a2", "m1", "B", "b1"));
    }

    @Test
    public void saveKeepsRequestsAddedByAnotherProcessAndAsksForReload() throws Exception {
        File file = Files.createTempFile("collections", ".json").toFile();
        CollectionDocumentJsonCodec.write(file, doc(group("A", request("a1"))));
        CollectionFilePersistence persistence = new CollectionFilePersistence(file.getPath());
        CollectionDocument loaded = persistence.loadOrCreate(CollectionDocument::empty);
        assertFalse(persistence.hasExternalChange());

        // Another process (the MCP server) adds m1; make sure the timestamp differs.
        Thread.sleep(20);
        CollectionDocumentJsonCodec.write(file, doc(group("A", request("a1"), request("m1"))));
        assertTrue(file.setLastModified(System.currentTimeMillis() + 5000));
        assertTrue(persistence.hasExternalChange(), "the watcher must notice the external write");

        // The app saves its own tree (it never saw m1) with a new request a2.
        assertTrue(persistence.save(doc(group("A", request("a1"), request("a2")))));

        assertEquals(CollectionExternalMerge.ids(CollectionDocumentJsonCodec.read(file)), Set.of("A", "a1", "a2", "m1"));
        assertTrue(persistence.hasExternalChange(), "the tree must reload to show m1");
        persistence.loadOrCreate(CollectionDocument::empty);
        assertFalse(persistence.hasExternalChange());
        assertEquals(CollectionExternalMerge.ids(loaded), Set.of("A", "a1"));
    }

    @Test
    public void writeIsAtomicAndLeavesNoTemporaryFiles() throws Exception {
        File directory = Files.createTempDirectory("collections-write").toFile();
        File file = new File(directory, "collections.json");
        CollectionDocumentJsonCodec.write(file, doc(group("A", request("a1"))));
        CollectionDocumentJsonCodec.write(file, doc(group("A", request("a1"), request("a2"))));

        assertEquals(CollectionExternalMerge.ids(CollectionDocumentJsonCodec.read(file)), Set.of("A", "a1", "a2"));
        assertEquals(directory.list().length, 1, String.join(",", directory.list()));
    }

    private static CollectionDocument doc(CollectionNode... roots) {
        return new CollectionDocument(new ArrayList<>(List.of(roots)));
    }

    private static CollectionNode group(String id, CollectionNode... children) {
        RequestGroup group = new RequestGroup(id);
        group.setId(id);
        CollectionNode node = CollectionNode.group(group);
        for (CollectionNode child : children) node.addChild(child);
        return node;
    }

    private static CollectionNode request(String id) {
        HttpRequestItem request = new HttpRequestItem();
        request.setId(id);
        request.setName(id);
        request.setUrl("https://example.test/" + id);
        return CollectionNode.request(request);
    }
}
