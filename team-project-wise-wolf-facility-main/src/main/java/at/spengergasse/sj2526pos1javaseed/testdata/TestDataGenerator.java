package at.spengergasse.sj2526pos1javaseed.testdata;

import at.spengergasse.sj2526pos1javaseed.Domain.Genre;
import at.spengergasse.sj2526pos1javaseed.Domain.Language;
import at.spengergasse.sj2526pos1javaseed.Domain.Permission;
import net.datafaker.Faker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * reproduzierbarer testdaten generator (gleicher seed = gleiche daten)
 *
 * - erzeugt das neutrale TestData modell, keine db abhängigkeit
 * - jeder datensatz bekommt seinen eigenen zufalls seed aus
 *   (seed, typ, index) -> ergebnis hängt NICHT von der chunk größe
 *   oder reihenfolge ab, kunden können stückweise erzeugt werden
 *   ohne alles im speicher zu halten
 * - fk sicherheit per konstruktion: ids fortlaufend, referenzen
 *   zeigen nur auf ids aus dem katalog
 * - vollständigkeit für den vergleich: jeder film/series ist in
 *   mind. einem store, jeder store hat mind. eine order
 *   -> alles was relational liegt kommt auch im mongo dokument vor
 *
 * ids: customer = index+1, billing/placement = customerId*10 + k,
 * order = ORDER_ID_OFFSET + customerId*10 + k (k < 10, max 2 billings / 3 placements)
 * order offset: BenchmarkRunner legt orders mit orderId = customerId oberhalb
 * von max(customer.id) an (bereich bis max + 100000), ohne offset würden
 * dessen orders mit den generierten kollidieren (order_placement.order_id unique)
 */
public class TestDataGenerator {

    public static final long DEFAULT_SEED = 42L;
    public static final long ORDER_ID_OFFSET = 1_000_000_000L;

    private static final long SALT_FILM = 1;
    private static final long SALT_SERIES = 2;
    private static final long SALT_STORE = 3;
    private static final long SALT_CUSTOMER = 4;
    private static final long SALT_TITLES = 5;

    private static final LocalDate ORDER_START = LocalDate.of(2020, 1, 1);
    private static final int ORDER_DAYS = 6 * 365;

    private static final String[] EMAIL_DOMAINS = {
            "gmx.at", "gmail.com", "aon.at", "chello.at", "outlook.com",
            "icloud.com", "a1.net", "web.de", "drei.at", "yahoo.de"
    };

    private static final Genre[] GENRES = Genre.values();
    private static final Language[] SECONDARY_LANGUAGES = {
            Language.French, Language.Spanish, Language.Japanese
    };

    /** österreichische städte mit echten plz, gewichtet nach größe */
    private static final City[] CITIES = {
            new City("Wien", 30, "1", viennaZips()),
            new City("Graz", 9, "316", "8010", "8020", "8036", "8041", "8042", "8045", "8051", "8053"),
            new City("Linz", 7, "732", "4020", "4030", "4040"),
            new City("Salzburg", 5, "662", "5020"),
            new City("Innsbruck", 4, "512", "6020"),
            new City("Klagenfurt", 3, "463", "9020"),
            new City("Villach", 2, "4242", "9500"),
            new City("Wels", 2, "7242", "4600"),
            new City("St. Pölten", 2, "2742", "3100"),
            new City("Dornbirn", 2, "5572", "6850"),
            new City("Wiener Neustadt", 2, "2622", "2700"),
            new City("Steyr", 1, "7252", "4400"),
            new City("Feldkirch", 1, "5522", "6800"),
            new City("Bregenz", 1, "5574", "6900"),
            new City("Leonding", 1, "732", "4060"),
            new City("Klosterneuburg", 1, "2243", "3400"),
            new City("Baden", 1, "2252", "2500"),
            new City("Wolfsberg", 1, "4352", "9400"),
            new City("Leoben", 1, "3842", "8700"),
            new City("Krems an der Donau", 1, "2732", "3500"),
            new City("Traun", 1, "7229", "4050"),
            new City("Amstetten", 1, "7472", "3300"),
            new City("Mödling", 1, "2236", "2340"),
            new City("Hallein", 1, "6245", "5400"),
            new City("Kufstein", 1, "5372", "6330"),
            new City("Eisenstadt", 1, "2682", "7000")
    };
    private static final int CITY_WEIGHT_TOTAL = sumWeights();

    private static final List<Title> FILM_TITLES = loadTitles("/testdata/film-titles.txt");
    private static final List<Title> SERIES_TITLES = loadTitles("/testdata/series-titles.txt");

    private final long seed;
    private final TestDataSizes sizes;
    private final List<Title> filmTitles;
    private final List<Title> seriesTitles;

    // ein random + faker, wird pro datensatz neu geseedet (setSeed ist billig,
    // faker neu bauen wäre teuer). nicht thread safe, generator ist single thread
    private final Random random = new Random();
    private final Faker faker = new Faker(Locale.forLanguageTag("de-AT"), random);

    public TestDataGenerator(long seed, int customerCount) {
        this.seed = seed;
        this.sizes = TestDataSizes.forCustomers(customerCount);
        this.filmTitles = shuffled(FILM_TITLES, mix(seed, SALT_TITLES, 1));
        this.seriesTitles = shuffled(SERIES_TITLES, mix(seed, SALT_TITLES, 2));
    }

    public long seed() {
        return seed;
    }

    public TestDataSizes sizes() {
        return sizes;
    }

    // ---------- katalog ----------

    public TestData.Catalog catalog() {
        List<TestData.Film> films = new ArrayList<>(sizes.films());
        for (int i = 0; i < sizes.films(); i++) {
            films.add(film(i));
        }
        List<TestData.Series> series = new ArrayList<>(sizes.series());
        for (int i = 0; i < sizes.series(); i++) {
            series.add(series(i));
        }
        List<TestData.Store> stores = new ArrayList<>(sizes.stores());
        for (int i = 0; i < sizes.stores(); i++) {
            stores.add(store(i));
        }
        return new TestData.Catalog(List.copyOf(films), List.copyOf(series), List.copyOf(stores));
    }

    private TestData.Film film(int index) {
        reseed(SALT_FILM, index);
        // titel liste ist pro seed gemischt, ab mehr filmen als titeln wiederholen
        // sich titel (remakes / neuauflagen), ids bleiben eindeutig
        Title title = filmTitles.get(index % filmTitles.size());
        return new TestData.Film(
                index + 1L,
                title.name(),
                title.year(),
                75 + random.nextInt(106),
                languages(),
                genres());
    }

    private TestData.Series series(int index) {
        reseed(SALT_SERIES, index);
        Title title = seriesTitles.get(index % seriesTitles.size());
        return new TestData.Series(
                index + 1L,
                title.name(),
                title.year(),
                6 + random.nextInt(115),
                20 + random.nextInt(46),
                languages(),
                genres());
    }

    private TestData.Store store(int index) {
        reseed(SALT_STORE, index);
        City city = CITIES[index % CITIES.length];
        String phone = "+43 " + city.areaCode() + " " + digits(city.areaCode().length() <= 1 ? 7 : 6);
        return new TestData.Store(
                index + 1L,
                phone,
                coveredIds(index, sizes.films(), sizes.filmsPerStore()),
                coveredIds(index, sizes.series(), sizes.seriesPerStore()));
    }

    /**
     * ids für einen store: pflicht anteil (alle ids mit (id-1) % stores == store)
     * damit jeder titel in mind. einem store ist, rest zufällig aufgefüllt
     */
    private List<Long> coveredIds(int storeIndex, int total, int perStore) {
        Set<Long> ids = new LinkedHashSet<>();
        for (int i = storeIndex; i < total; i += sizes.stores()) {
            ids.add(i + 1L);
        }
        while (ids.size() < perStore) {
            ids.add(1L + random.nextInt(total));
        }
        return List.copyOf(ids);
    }

    // ---------- kunden ----------

    /** kunden mit index [from, to), unabhängig davon wie man die bereiche aufteilt */
    public List<TestData.Customer> customers(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > sizes.customers() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("bereich [" + fromIndex + ", " + toIndex
                    + ") außerhalb von 0.." + sizes.customers());
        }
        List<TestData.Customer> out = new ArrayList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            out.add(customer(i));
        }
        return out;
    }

    private TestData.Customer customer(int index) {
        long id = index + 1L;
        reseed(SALT_CUSTOMER, index);

        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        // id im nickname/email -> garantiert eindeutig (unique constraints)
        String handle = slug(firstName) + "." + slug(lastName) + id;
        String nickname = handle;
        String email = handle + "@" + EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];

        int p = random.nextInt(100);
        Permission permission = p < 3 ? Permission.Admin : p < 10 ? Permission.Employee : Permission.Customer;

        City city = pickCity();
        String zip = city.zips()[random.nextInt(city.zips().length)];
        String street = faker.address().streetName() + " " + houseNumber();

        int billingCount = random.nextInt(10) < 3 ? 2 : 1;
        List<TestData.Billing> billings = new ArrayList<>(billingCount);
        for (int k = 0; k < billingCount; k++) {
            billings.add(new TestData.Billing(
                    id * 10 + k,
                    cardNumber(),
                    LocalDate.of(2026 + random.nextInt(6), 1 + random.nextInt(12), 1),
                    100 + random.nextInt(900)));
        }

        int placementCount = 1 + random.nextInt(3);
        List<TestData.Placement> placements = new ArrayList<>(placementCount);
        for (int k = 0; k < placementCount; k++) {
            // erstes placement verteilt kunden reihum auf stores, damit jeder
            // store mind. eine order hat (sonst fehlt er im mongo modell)
            long storeId = k == 0
                    ? (index % sizes.stores()) + 1L
                    : 1L + random.nextInt(sizes.stores());
            LocalDateTime orderDate = ORDER_START
                    .plusDays(random.nextInt(ORDER_DAYS))
                    .atTime(9, 0)
                    .plusMinutes(random.nextInt(12 * 60));
            BigDecimal total = BigDecimal.valueOf(299 + random.nextInt(4701), 2);
            TestData.Order order = new TestData.Order(ORDER_ID_OFFSET + id * 10 + k, orderDate, total, storeId);

            LocalDate rentDate = orderDate.toLocalDate();
            int rentDays = new int[]{3, 7, 14}[random.nextInt(3)];
            LocalDate actualReturn = random.nextInt(100) < 15
                    ? null // noch nicht zurückgegeben
                    : rentDate.plusDays(1 + random.nextInt(rentDays + 7));
            placements.add(new TestData.Placement(
                    id * 10 + k, rentDate, rentDate.plusDays(rentDays), actualReturn, order));
        }

        return new TestData.Customer(id, nickname, email, firstName, lastName, permission,
                street, city.name(), zip, "Austria", List.copyOf(billings), List.copyOf(placements));
    }

    // ---------- helpers ----------

    private void reseed(long salt, long index) {
        random.setSeed(mix(seed, salt, index));
    }

    /** splitmix64, verteilt (seed, salt, index) auf unabhängige seeds */
    static long mix(long seed, long salt, long index) {
        long z = splitmix(seed + salt * 0x9E3779B97F4A7C15L);
        return splitmix(z + index * 0x9E3779B97F4A7C15L);
    }

    private static long splitmix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private List<Language> languages() {
        EnumSet<Language> set = EnumSet.noneOf(Language.class);
        int r = random.nextInt(100);
        set.add(r < 55 ? Language.English
                : r < 80 ? Language.German
                : SECONDARY_LANGUAGES[random.nextInt(SECONDARY_LANGUAGES.length)]);
        // deutsche synchro ist in österreich der normalfall
        if (random.nextInt(100) < 75) {
            set.add(Language.German);
        }
        if (random.nextInt(100) < 20) {
            set.add(Language.English);
        }
        return List.copyOf(set);
    }

    private List<Genre> genres() {
        EnumSet<Genre> set = EnumSet.noneOf(Genre.class);
        int count = 1 + random.nextInt(3);
        while (set.size() < count) {
            set.add(GENRES[random.nextInt(GENRES.length)]);
        }
        return List.copyOf(set);
    }

    private City pickCity() {
        int r = random.nextInt(CITY_WEIGHT_TOTAL);
        for (City city : CITIES) {
            r -= city.weight();
            if (r < 0) {
                return city;
            }
        }
        return CITIES[0];
    }

    private String houseNumber() {
        int nr = 1 + random.nextInt(120);
        return random.nextInt(100) < 40 ? nr + "/" + (1 + random.nextInt(25)) : String.valueOf(nr);
    }

    private String digits(int count) {
        StringBuilder sb = new StringBuilder(count);
        sb.append(1 + random.nextInt(9));
        for (int i = 1; i < count; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /** 16 stellige karten nummer (visa 4 / mastercard 51-55) mit gültiger luhn prüfziffer */
    private String cardNumber() {
        int[] d = new int[16];
        if (random.nextBoolean()) {
            d[0] = 4;
            d[1] = random.nextInt(10);
        } else {
            d[0] = 5;
            d[1] = 1 + random.nextInt(5);
        }
        for (int i = 2; i < 15; i++) {
            d[i] = random.nextInt(10);
        }
        int sum = 0;
        for (int i = 14; i >= 0; i--) {
            int v = d[i];
            if ((14 - i) % 2 == 0) {
                v *= 2;
                if (v > 9) v -= 9;
            }
            sum += v;
        }
        d[15] = (10 - sum % 10) % 10;
        StringBuilder sb = new StringBuilder(16);
        for (int v : d) sb.append(v);
        return sb.toString();
    }

    /** "Jürgen" -> "juergen", nur [a-z0-9] damit email gültig bleibt */
    static String slug(String value) {
        String s = value.toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^a-z0-9]", "");
        return s.isEmpty() ? "user" : s;
    }

    private static List<Title> shuffled(List<Title> titles, long seed) {
        List<Title> copy = new ArrayList<>(titles);
        Collections.shuffle(copy, new Random(seed));
        return List.copyOf(copy);
    }

    private static List<Title> loadTitles(String resource) {
        try (InputStream in = TestDataGenerator.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("titel liste fehlt: " + resource);
            }
            List<Title> titles = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int sep = line.lastIndexOf(';');
                titles.add(new Title(line.substring(0, sep).strip(),
                        Integer.parseInt(line.substring(sep + 1).strip())));
            }
            if (titles.isEmpty()) {
                throw new IllegalStateException("titel liste leer: " + resource);
            }
            return List.copyOf(titles);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String[] viennaZips() {
        String[] zips = new String[23];
        for (int d = 1; d <= 23; d++) {
            zips[d - 1] = String.format("1%02d0", d);
        }
        return zips;
    }

    private static int sumWeights() {
        int total = 0;
        for (City c : CITIES) total += c.weight();
        return total;
    }

    private record Title(String name, int year) {
    }

    private record City(String name, int weight, String areaCode, String... zips) {
    }
}
