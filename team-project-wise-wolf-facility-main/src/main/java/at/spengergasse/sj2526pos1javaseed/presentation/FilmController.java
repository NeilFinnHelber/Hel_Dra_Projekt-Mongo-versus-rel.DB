package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateFilmCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Film;
import at.spengergasse.sj2526pos1javaseed.Ids.FilmId;
import at.spengergasse.sj2526pos1javaseed.dto.FilmDTO;
import at.spengergasse.sj2526pos1javaseed.services.FilmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.NoSuchElementException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1.0.0/filme")
@Validated
@Tag(name = "Film", description = "Endpoints for managing films")
public class FilmController {

    private final FilmService filmService;
    private final PagedResourcesAssembler<FilmDTO> pagedResourcesAssembler;

    // neuen film erstellen und als hateoas-model zurückgeben
    @Operation(summary = "Create film", description = "Create film operation")
    @PostMapping
    public ResponseEntity<EntityModel<FilmDTO>> createFilm(@Valid @RequestBody CreateFilmCommand cmd) {
        Film film = filmService.create(cmd);
        FilmDTO dto = FilmDTO.from(film);
        EntityModel<FilmDTO> entityModel = EntityModel.of(dto,
                linkTo(methodOn(FilmController.class).getFilm(dto.id())).withSelfRel(),
                linkTo(methodOn(FilmController.class).getAllFilms(null)).withRel("films")
        );
        return ResponseEntity.ok(entityModel);
    }

    // einen einzelnen film anhand der id laden
    @Operation(summary = "Get film", description = "Get film operation")
    @GetMapping("/{filmId}")
    public ResponseEntity<EntityModel<FilmDTO>> getFilm(@PathVariable long filmId) {
        try {
            Film film = filmService.getFilmById(new FilmId(filmId));
            FilmDTO dto = FilmDTO.from(film);
            EntityModel<FilmDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(FilmController.class).getFilm(filmId)).withSelfRel(),
                    linkTo(methodOn(FilmController.class).getAllFilms(null)).withRel("films")
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // alle filme seitenweise laden
    @Operation(summary = "Get all films", description = "Get all films operation")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<FilmDTO>>> getAllFilms(Pageable pageable) {
        try {
            Page<FilmDTO> page = filmService.getAllFilms(pageable).map(FilmDTO::from);
            PagedModel<EntityModel<FilmDTO>> pagedModel = pagedResourcesAssembler.toModel(page,
                    dto -> EntityModel.of(dto,
                            linkTo(methodOn(FilmController.class).getFilm(dto.id())).withSelfRel()
                    )
            );
            return ResponseEntity.ok(pagedModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // vorhandenen film aktualisieren (felder aus dem request übernehmen)
    @Operation(summary = "Update film", description = "Update film operation")
    @PatchMapping("/{filmId}")
    public ResponseEntity<EntityModel<FilmDTO>> updateFilm(@PathVariable Long filmId, @RequestBody CreateFilmCommand cmd) {
        try {
            Film film = filmService.getFilmById(new FilmId(filmId));
            film.setTitle(cmd.title());
            film.setRelease_year(cmd.releaseYear());
            film.setLength(cmd.length());
            film.setLanguages(cmd.languages());
            film.setGenres(cmd.genres());
            Film updated = filmService.updateProduct(film);
            if (updated == null) {
                return ResponseEntity.noContent().build();
            }
            FilmDTO dto = FilmDTO.from(updated);
            EntityModel<FilmDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(FilmController.class).getFilm(filmId)).withSelfRel()
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // film löschen (entfernt ihn auch aus allen stores)
    @Operation(summary = "Delete film", description = "Delete film operation")
    @DeleteMapping("/{filmId}")
    public ResponseEntity<Void> deleteFilm(@PathVariable long filmId) {
        filmService.deleteFilm(filmId);
        return ResponseEntity.ok().build();
    }

    // poster-bild eines films als binärdaten zurückgeben
    @Operation(summary = "Get film poster", description = "Get the poster image of a film")
    @GetMapping(value = "/{filmId}/poster", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> getFilmPoster(@PathVariable long filmId) {
        try {
            Film film = filmService.getFilmById(new FilmId(filmId));
            if (film.getPoster() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(film.getPoster());
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // poster-bild für einen film hochladen und speichern
    @Operation(summary = "Upload film poster", description = "Upload the poster image of a film")
    @PostMapping(value = "/{filmId}/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadFilmPoster(@PathVariable long filmId, @RequestParam("file") MultipartFile file) {
        try {
            Film film = filmService.getFilmById(new FilmId(filmId));
            film.setPoster(file.getBytes());
            filmService.updateProduct(film);
            return ResponseEntity.ok().build();
        } catch (IOException | NoSuchElementException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
