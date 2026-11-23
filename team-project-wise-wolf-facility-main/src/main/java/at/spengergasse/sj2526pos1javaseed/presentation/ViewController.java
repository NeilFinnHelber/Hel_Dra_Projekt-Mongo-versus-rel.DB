package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.dto.*;
import at.spengergasse.sj2526pos1javaseed.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Tag(name = "View", description = "Endpoints for managing views")
public class ViewController {

    private final FilmService filmService;
    private final SeriesService seriesService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final StoreService storeService;

    // startseite: zählt alle einträge und zeigt die übersicht
    @Operation(summary = "Index", description = "Index operation")
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("filmCount", filmService.getAllFilms().size());
        model.addAttribute("seriesCount", seriesService.getAll().size());
        model.addAttribute("customerCount", customerService.getAll().size());
        model.addAttribute("orderCount", orderService.getAll().size());
        model.addAttribute("storeCount", storeService.getAll().size());
        return "index";
    }

    // filmseite: alle filme laden und ans template übergeben
    @Operation(summary = "Films", description = "Films operation")
    @GetMapping("/films")
    public String films(Model model) {
        model.addAttribute("films", filmService.getAllFilms().stream().map(FilmDTO::from).toList());
        return "films";
    }

    // serienSeite: alle serien laden und ans template übergeben
    @Operation(summary = "Series", description = "Series operation")
    @GetMapping("/series")
    public String series(Model model) {
        model.addAttribute("seriesList", seriesService.getAll().stream().map(SeriesDTO::from).toList());
        return "series";
    }

    // kundenseite: alle kunden laden und ans template übergeben
    @Operation(summary = "Customers", description = "Customers operation")
    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", customerService.getAll().stream().map(CustomerDTO::from).toList());
        return "customers";
    }

    // bestellungsseite: bestellungen und stores laden (store-auswahl für neue bestellungen)
    @Operation(summary = "Orders", description = "Orders operation")
    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderService.getAll().stream().map(OrderDTO::from).toList());
        model.addAttribute("stores", storeService.getAll().stream().map(StoreDTO::from).toList());
        return "orders";
    }

    // store-seite: stores, filme und serien laden (für die verknüpfungsauswahl)
    @Operation(summary = "Stores", description = "Stores operation")
    @GetMapping("/stores")
    public String stores(Model model) {
        model.addAttribute("stores", storeService.getAll().stream().map(StoreDTO::from).toList());
        model.addAttribute("films", filmService.getAllFilms().stream().map(FilmDTO::from).toList());
        model.addAttribute("seriesList", seriesService.getAll().stream().map(SeriesDTO::from).toList());
        return "stores";
    }

    // login-seite anzeigen
    @Operation(summary = "Login", description = "Renders the login page")
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // weiterleitung zur statischen asciidoc-dokumentation
    @Operation(summary = "Documentation", description = "Redirects to the generated AsciiDoc HTML")
    @GetMapping("/docs")
    public String docs() {
        return "redirect:/docs/index.html";
    }
}
