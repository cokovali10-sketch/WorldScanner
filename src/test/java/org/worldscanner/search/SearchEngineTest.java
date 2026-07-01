package org.worldscanner.search;

import org.junit.jupiter.api.Test;
import org.worldscanner.anvil.ChunkData;
import org.worldscanner.model.DimensionType;
import org.worldscanner.model.SearchResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SearchEngineTest {

    @Test
    void searchBlocksFindsMatchingBlocksInRawChunkPayload() {
        byte[] payload = "minecraft:diamond_ore".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        ChunkData chunkData = new ChunkData(0, 0, payload);
        List<SearchResult> results = SearchEngine.searchBlocks(chunkData, "diamond_ore", DimensionType.OVERWORLD, 0, 0, new SearchFilter(DimensionType.OVERWORLD, null, null, null, null, null, null));

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertNotNull(results.get(0).getBlockId());
        assertEquals("diamond_ore", results.get(0).getBlockId().toLowerCase(java.util.Locale.ROOT));
    }
}
