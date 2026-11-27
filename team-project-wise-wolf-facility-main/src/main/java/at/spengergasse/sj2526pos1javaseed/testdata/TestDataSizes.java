package at.spengergasse.sj2526pos1javaseed.testdata;

/**
 * mengengerüst vom testdaten generator
 *
 * primäre größe = anzahl kunden (customers)
 * - 1 kunde = 1 mongo dokument = 1 zeile in customer
 * - alles andere wird daraus abgeleitet (feste verhältnisse):
 *
 * <pre>
 *   stores          = max(3,  ceil(customers / 1000))
 *   films           = max(20, ceil(customers / 200))
 *   series          = max(10, films / 2)
 *   films pro store = max(10, ceil(films / stores))   (jeder film in mind. 1 store)
 *   series pro store= max(5,  ceil(series / stores))
 *   pro kunde       : 1-2 billing infos, 1-3 placements (je 1 order)
 * </pre>
 *
 * katalog bleibt absichtlich klein: im mongo modell steckt in jedem
 * placement der komplette store katalog, ein großer katalog würde
 * die dokumente aufblasen (100k kunden ~ 6 kb pro dokument)
 */
public record TestDataSizes(int customers, int films, int series, int stores,
                            int filmsPerStore, int seriesPerStore) {

    public static final int MIN_CUSTOMERS = 10;
    public static final int MAX_CUSTOMERS = 100_000;

    public static TestDataSizes forCustomers(int customers) {
        if (customers < MIN_CUSTOMERS || customers > MAX_CUSTOMERS) {
            throw new IllegalArgumentException("app.testdata.count muss zwischen "
                    + MIN_CUSTOMERS + " und " + MAX_CUSTOMERS + " liegen, war " + customers);
        }
        int stores = Math.max(3, ceilDiv(customers, 1000));
        int films = Math.max(20, ceilDiv(customers, 200));
        int series = Math.max(10, films / 2);
        int filmsPerStore = Math.min(films, Math.max(10, ceilDiv(films, stores)));
        int seriesPerStore = Math.min(series, Math.max(5, ceilDiv(series, stores)));
        return new TestDataSizes(customers, films, series, stores, filmsPerStore, seriesPerStore);
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
