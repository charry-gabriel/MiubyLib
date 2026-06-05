package fr.miuby.lib.utils;

import org.bukkit.Location;

/**
 * Zone 3D définie par ses bornes sur chaque axe.
 *
 * <p><b>Création recommandée :</b> {@link #of(Location, Location)} — les min/max sont déduits
 * automatiquement, l'ordre des points n'a pas d'importance.</p>
 *
 * <p><b>Constructeur direct :</b> {@code new Rect(xMax, xMin, yMax, yMin, zMax, zMin)} —
 * l'ordre max-avant-min est obligatoire ; une incohérence lève une {@link IllegalArgumentException}.</p>
 */
public record Rect(int xMax, int xMin, int yMax, int yMin, int zMax, int zMin) {

    public Rect {
        if (xMax < xMin) throw new IllegalArgumentException("Rect: xMax (" + xMax + ") < xMin (" + xMin + ")");
        if (yMax < yMin) throw new IllegalArgumentException("Rect: yMax (" + yMax + ") < yMin (" + yMin + ")");
        if (zMax < zMin) throw new IllegalArgumentException("Rect: zMax (" + zMax + ") < zMin (" + zMin + ")");
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Crée un {@code Rect} depuis deux {@link Location} Bukkit.
     * Les bornes sont calculées automatiquement — l'ordre des points est sans importance.
     *
     * @param a premier point de la zone
     * @param b deuxième point de la zone
     * @return le {@code Rect} délimitant la zone entre les deux points
     */
    public static Rect of(Location a, Location b) {
        int ax = a.getBlockX(), bx = b.getBlockX();
        int ay = a.getBlockY(), by = b.getBlockY();
        int az = a.getBlockZ(), bz = b.getBlockZ();
        return new Rect(
                Math.max(ax, bx), Math.min(ax, bx),
                Math.max(ay, by), Math.min(ay, by),
                Math.max(az, bz), Math.min(az, bz)
        );
    }

    // -------------------------------------------------------------------------
    // Tests de position
    // -------------------------------------------------------------------------

    /**
     * Indique si le point (x, y, z) est hors de la zone.
     *
     * @param x coordonnée X
     * @param y coordonnée Y
     * @param z coordonnée Z
     * @return {@code true} si le point est hors de la zone
     */
    public boolean isOut(int x, int y, int z) {
        return x > xMax || x < xMin || y > yMax || y < yMin || z > zMax || z < zMin;
    }

    /**
     * Indique si la {@link Location} est hors de la zone.
     *
     * @param location la position à vérifier
     * @return {@code true} si la position est hors de la zone
     */
    public boolean isOut(Location location) {
        return isOut(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Indique si le point (x, y, z) est dans la zone.
     *
     * @param x coordonnée X
     * @param y coordonnée Y
     * @param z coordonnée Z
     * @return {@code true} si le point est dans la zone
     */
    public boolean contains(int x, int y, int z) {
        return !isOut(x, y, z);
    }

    /**
     * Indique si la {@link Location} est dans la zone.
     *
     * @param location la position à vérifier
     * @return {@code true} si la position est dans la zone
     */
    public boolean contains(Location location) {
        return !isOut(location);
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    /**
     * Retourne un nouveau {@code Rect} agrandi de {@code amount} blocs dans toutes les directions.
     * Une valeur négative rétrécit la zone (lève une exception si les bornes se croisent).
     *
     * @param amount nombre de blocs d'agrandissement (peut être négatif pour rétrécir)
     * @return nouveau {@code Rect} agrandi
     */
    public Rect expand(int amount) {
        return new Rect(
                xMax + amount, xMin - amount,
                yMax + amount, yMin - amount,
                zMax + amount, zMin - amount
        );
    }
}
