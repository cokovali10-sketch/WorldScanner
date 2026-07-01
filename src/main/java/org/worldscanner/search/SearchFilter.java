package org.worldscanner.search;

import org.worldscanner.model.DimensionType;

import java.util.List;
import java.util.Locale;

/**
 * Фильтр поиска по измерению, региону и радиусу.
 */
public final class SearchFilter {

    private final DimensionType dimension;
    private final Integer regionX;
    private final Integer regionZ;
    private final Integer centerX;
    private final Integer centerY;
    private final Integer centerZ;
    private final Integer radius;
    private final Integer limit;
    private final boolean showSummary;

    public SearchFilter(DimensionType dimension, Integer regionX, Integer regionZ,
                        Integer centerX, Integer centerY, Integer centerZ, Integer radius) {
        this.dimension = dimension == null ? DimensionType.UNKNOWN : dimension;
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.limit = null;
        this.showSummary = false;
    }

    public SearchFilter(DimensionType dimension, Integer regionX, Integer regionZ,
                        Integer centerX, Integer centerY, Integer centerZ, Integer radius,
                        Integer limit, boolean showSummary) {
        this.dimension = dimension == null ? DimensionType.UNKNOWN : dimension;
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.limit = limit;
        this.showSummary = showSummary;
    }

    public static SearchFilter fromOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return new SearchFilter(DimensionType.UNKNOWN, null, null, null, null, null, null);
        }
        DimensionType dimension = DimensionType.UNKNOWN;
        Integer regionX = null;
        Integer regionZ = null;
        Integer centerX = null;
        Integer centerY = null;
        Integer centerZ = null;
        Integer radius = null;
        Integer limit = null;
        boolean showSummary = false;

        for (String option : options) {
            if (option == null || option.isBlank()) {
                continue;
            }
            String normalized = option.trim();
            if (normalized.startsWith("--dimension=")) {
                dimension = parseDimension(normalized.substring(normalized.indexOf('=') + 1));
                continue;
            }
            if (normalized.startsWith("--dimension")) {
                String[] parts = normalized.split("=", 2);
                if (parts.length == 2) {
                    dimension = parseDimension(parts[1]);
                }
                continue;
            }
            if (normalized.startsWith("--region=")) {
                String[] coords = normalized.substring(normalized.indexOf('=') + 1).split(",");
                if (coords.length == 2) {
                    regionX = parseIntOrNull(coords[0]);
                    regionZ = parseIntOrNull(coords[1]);
                }
                continue;
            }
            if (normalized.startsWith("--coords=")) {
                String[] values = normalized.substring(normalized.indexOf('=') + 1).split(",");
                if (values.length == 3) {
                    centerX = parseIntOrNull(values[0]);
                    centerY = parseIntOrNull(values[1]);
                    centerZ = parseIntOrNull(values[2]);
                }
                continue;
            }
            if (normalized.startsWith("--radius=")) {
                radius = parseIntOrNull(normalized.substring(normalized.indexOf('=') + 1));
                continue;
            }
            if (normalized.startsWith("--limit=")) {
                limit = parseIntOrNull(normalized.substring(normalized.indexOf('=') + 1));
                continue;
            }
            if (normalized.equals("--summary") || normalized.equals("--summary=true")) {
                showSummary = true;
            }
        }

        return new SearchFilter(dimension, regionX, regionZ, centerX, centerY, centerZ, radius, limit, showSummary);
    }

    private static DimensionType parseDimension(String dimension) {
        if (dimension == null) {
            return DimensionType.UNKNOWN;
        }
        return switch (dimension.toLowerCase(Locale.ROOT)) {
            case "overworld", "world" -> DimensionType.OVERWORLD;
            case "nether", "dim-1" -> DimensionType.NETHER;
            case "the_end", "end", "dim1" -> DimensionType.END;
            default -> DimensionType.UNKNOWN;
        };
    }

    private static Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean matchesDimension(DimensionType current) {
        return dimension == DimensionType.UNKNOWN || current == dimension;
    }

    public boolean matchesRegion(int regionX, int regionZ) {
        if (this.regionX == null || this.regionZ == null) {
            return true;
        }
        return this.regionX == regionX && this.regionZ == regionZ;
    }

    public boolean matchesPosition(int x, int y, int z) {
        if (centerX == null || centerY == null || centerZ == null || radius == null) {
            return true;
        }
        int deltaX = x - centerX;
        int deltaY = y - centerY;
        int deltaZ = z - centerZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= radius * radius;
    }

    public int getLimit() {
        return limit == null ? Integer.MAX_VALUE : limit;
    }

    public boolean isShowSummary() {
        return showSummary;
    }
}
