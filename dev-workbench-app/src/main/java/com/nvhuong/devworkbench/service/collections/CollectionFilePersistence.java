package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class CollectionFilePersistence {
    private static final ConcurrentHashMap<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> LOADING_STATUS = new ConcurrentHashMap<>();

    private String filePath;
    /** Size and modification time of collections.json after our last load or save. */
    private volatile String lastStamp = "";
    /** Node ids of the document we last loaded or saved: the base for the three-way merge in save(). */
    private volatile java.util.Set<String> baseIds = java.util.Set.of();
    private volatile boolean reloadRequested;

    public CollectionFilePersistence(String filePath) {
        this.filePath = filePath;
    }

    public CollectionDocument loadOrCreate(Supplier<CollectionDocument> defaultDocumentSupplier) throws IOException {
        synchronized (fileLock()) {
            setFileLoading(true);
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    CollectionDocument defaultDocument = defaultDocument(defaultDocumentSupplier);
                    saveIgnoringLoadGuard(defaultDocument);
                    remember(defaultDocument);
                    log.info("Collection file not found, created default document: {}", filePath);
                    return defaultDocument;
                }
                CollectionDocument document = CollectionDocumentJsonCodec.read(file);
                remember(document);
                return document;
            } finally {
                setFileLoading(false);
            }
        }
    }

    public boolean save(CollectionDocument document) {
        if (isFileLoading()) {
            log.warn("Skipping save operation for file '{}' because it is being loaded", filePath);
            return false;
        }

        synchronized (fileLock()) {
            if (isFileLoading()) {
                log.warn("Skipping save operation for file '{}' because it is being loaded (double-check)", filePath);
                return false;
            }
            try {
                CollectionDocument toSave = document;
                boolean mergedExternalAdditions = false;
                File file = new File(filePath);
                if (file.isFile() && !lastStamp.isEmpty() && !stamp(file).equals(lastStamp)) {
                    // collections.json changed outside the app (e.g. the MCP server added a request): keep those
                    // additions instead of overwriting them with the in-memory tree.
                    CollectionExternalMerge.Result merged = CollectionExternalMerge.keepExternalAdditions(
                            document, CollectionDocumentJsonCodec.read(file), baseIds);
                    toSave = merged.document();
                    if (merged.added() > 0) {
                        mergedExternalAdditions = true;
                        log.info("Kept {} collection item(s) added outside the app in {}", merged.added(), filePath);
                    }
                }
                saveIgnoringLoadGuard(toSave);
                remember(toSave);
                // The tree on screen does not show the merged items yet: ask the UI to reload.
                reloadRequested = mergedExternalAdditions;
                log.debug("Saved collection document to: {}", filePath);
                return true;
            } catch (Exception ex) {
                log.error("Error saving collection document to file: {}", filePath, ex);
                return false;
            }
        }
    }

    public void export(CollectionDocument document, File fileToSave) throws IOException {
        CollectionDocumentJsonCodec.write(fileToSave, document);
    }

    public CollectionDocument switchFilePathAndLoad(String path,
                                                    Supplier<CollectionDocument> defaultDocumentSupplier) throws IOException {
        if (path == null || path.isBlank()) {
            return loadOrCreate(defaultDocumentSupplier);
        }

        String oldPath = this.filePath;
        synchronized (FILE_LOCKS.computeIfAbsent(oldPath, ignored -> new Object())) {
            this.filePath = path;
            return loadOrCreate(defaultDocumentSupplier);
        }
    }

    /**
     * True when collections.json was changed by another process since the app last loaded or saved it, or when a
     * save had to merge in such changes; the caller should reload the tree.
     */
    public boolean hasExternalChange() {
        if (isFileLoading()) return false;
        if (reloadRequested) return true;
        File file = new File(filePath);
        return file.isFile() && !lastStamp.isEmpty() && !stamp(file).equals(lastStamp);
    }

    private void remember(CollectionDocument document) {
        lastStamp = stamp(new File(filePath));
        baseIds = CollectionExternalMerge.ids(document);
        reloadRequested = false;
    }

    private static String stamp(File file) {
        try {
            var attributes = java.nio.file.Files.readAttributes(file.toPath(),
                    java.nio.file.attribute.BasicFileAttributes.class);
            return attributes.size() + ":" + attributes.lastModifiedTime().toMillis();
        } catch (IOException missing) {
            return "";
        }
    }

    private void saveIgnoringLoadGuard(CollectionDocument document) throws IOException {
        CollectionDocumentJsonCodec.write(new File(filePath), document);
    }

    private CollectionDocument defaultDocument(Supplier<CollectionDocument> defaultDocumentSupplier) {
        if (defaultDocumentSupplier == null) {
            return CollectionDocument.empty();
        }
        CollectionDocument document = defaultDocumentSupplier.get();
        return document == null ? CollectionDocument.empty() : document;
    }

    private Object fileLock() {
        return FILE_LOCKS.computeIfAbsent(filePath, ignored -> new Object());
    }

    private boolean isFileLoading() {
        return LOADING_STATUS.getOrDefault(filePath, false);
    }

    private void setFileLoading(boolean loading) {
        if (loading) {
            LOADING_STATUS.put(filePath, true);
        } else {
            LOADING_STATUS.remove(filePath);
        }
    }
}
