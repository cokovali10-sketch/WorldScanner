package org.worldscanner.cli;

import org.junit.jupiter.api.Test;
import org.worldscanner.search.SearchFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineTest {

    @Test
    void parsesConvenientScanSyntax() {
        CommandLine command = CommandLine.parse(new String[]{"C:/world", "scan", "--dimension=nether"});

        assertNotNull(command);
        assertEquals(CommandLine.CommandType.SCAN, command.getCommandType());
        assertEquals("C:/world", command.getWorldPath());
        assertTrue(command.getExtraOptions().contains("--dimension=nether"));
    }

    @Test
    void parsesLimitAndSummaryOptions() {
        SearchFilter filter = SearchFilter.fromOptions(List.of("--limit=5", "--summary"));

        assertEquals(5, filter.getLimit());
        assertTrue(filter.isShowSummary());
    }
}
