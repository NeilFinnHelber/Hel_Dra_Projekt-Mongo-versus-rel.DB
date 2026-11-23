package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateSeriesCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Series;
import at.spengergasse.sj2526pos1javaseed.Ids.SeriesId;
import at.spengergasse.sj2526pos1javaseed.dto.SeriesDTO;
import at.spengergasse.sj2526pos1javaseed.services.SeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.NoSuchElementException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1.0.0/series")
@Validated
@Tag(name = "Series", description = "Endpoints for managing seriess")
public class SeriesController {

    private final SeriesService seriesService;
    private final PagedResourcesAssembler<SeriesDTO> pagedResourcesAssembler;

    // neue serie erstellen und als hateoas-model zurückgeben
    @Operation(summary = "Create series", description = "Create series operation")
    @PostMapping
    public ResponseEntity<EntityModel<SeriesDTO>> createSeries(@Valid @RequestBody CreateSeriesCommand cmd) {
        Series series = seriesService.create(cmd);
        SeriesDTO dto = SeriesDTO.from(series);
        EntityModel<SeriesDTO> entityModel = EntityModel.of(dto,
                linkTo(methodOn(SeriesController.class).getSeries(dto.id())).withSelfRel(),
                linkTo(methodOn(SeriesController.class).getAllSeries(null)).withRel("seriesList")
        );
        return ResponseEntity.ok(entityModel);
    }

    // eine einzelne serie anhand der id laden
    @Operation(summary = "Get series", description = "Get series operation")
    @GetMapping("/{seriesId}")
    public ResponseEntity<EntityModel<SeriesDTO>> getSeries(@PathVariable Long seriesId) {
        try {
            Series series = seriesService.getById(new SeriesId(seriesId));
            SeriesDTO dto = SeriesDTO.from(series);
            EntityModel<SeriesDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(SeriesController.class).getSeries(seriesId)).withSelfRel(),
                    linkTo(methodOn(SeriesController.class).getAllSeries(null)).withRel("seriesList")
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // alle serien seitenweise laden
    @Operation(summary = "Get all series", description = "Get all series operation")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<SeriesDTO>>> getAllSeries(Pageable pageable) {
        Page<SeriesDTO> page = seriesService.getAll(pageable).map(SeriesDTO::from);
        PagedModel<EntityModel<SeriesDTO>> pagedModel = pagedResourcesAssembler.toModel(page,
                dto -> EntityModel.of(dto,
                        linkTo(methodOn(SeriesController.class).getSeries(dto.id())).withSelfRel()
                )
        );
        return ResponseEntity.ok(pagedModel);
    }

    // vorhandene serie aktualisieren (felder aus dem request übernehmen)
    @Operation(summary = "Update series", description = "Update series operation")
    @PatchMapping("/{seriesId}")
    public ResponseEntity<EntityModel<SeriesDTO>> updateSeries(@PathVariable Long seriesId,
                                                               @RequestBody CreateSeriesCommand cmd) {
        try {
            Series series = seriesService.getById(new SeriesId(seriesId));
            series.setTitle(cmd.title());
            series.setRelease_year(cmd.releaseYear());
            series.setEpisodes(cmd.episodes());
            series.setLength(cmd.length());
            series.setLanguages(cmd.languages());
            series.setGenres(cmd.genres());
            seriesService.updateSeries(series);
            SeriesDTO dto = SeriesDTO.from(series);
            EntityModel<SeriesDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(SeriesController.class).getSeries(seriesId)).withSelfRel()
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // serie löschen (entfernt sie auch aus allen stores)
    @Operation(summary = "Delete series", description = "Delete series operation")
    @DeleteMapping("/{seriesId}")
    public ResponseEntity<Void> deleteSeries(@PathVariable Long seriesId) {
        seriesService.deleteSeries(new SeriesId(seriesId));
        return ResponseEntity.noContent().build();
    }
}
