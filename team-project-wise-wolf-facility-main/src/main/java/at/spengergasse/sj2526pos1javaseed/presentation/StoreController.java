package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateStoreCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Store;
import at.spengergasse.sj2526pos1javaseed.Ids.StoreId;
import at.spengergasse.sj2526pos1javaseed.dto.StoreDTO;
import at.spengergasse.sj2526pos1javaseed.services.StoreService;
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
@RequestMapping("/api/v1.0.0/stores")
@Validated
@Tag(name = "Store", description = "Endpoints for managing stores")
public class StoreController {

    private final StoreService storeService;
    private final PagedResourcesAssembler<StoreDTO> pagedResourcesAssembler;

    @Operation(summary = "Create store", description = "Create store operation")
    @PostMapping
    public ResponseEntity<EntityModel<StoreDTO>> createStore(@Valid @RequestBody CreateStoreCommand cmd) {
        Store store = storeService.create(cmd);
        StoreDTO dto = StoreDTO.from(store);
        EntityModel<StoreDTO> entityModel = EntityModel.of(dto,
                linkTo(methodOn(StoreController.class).getStore(dto.id())).withSelfRel(),
                linkTo(methodOn(StoreController.class).getAllStores(null)).withRel("stores")
        );
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Get store", description = "Get store operation")
    @GetMapping("/{storeId}")
    public ResponseEntity<EntityModel<StoreDTO>> getStore(@PathVariable Long storeId) {
        try {
            Store store = storeService.getById(new StoreId(storeId));
            StoreDTO dto = StoreDTO.from(store);
            EntityModel<StoreDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(StoreController.class).getStore(storeId)).withSelfRel(),
                    linkTo(methodOn(StoreController.class).getAllStores(null)).withRel("stores")
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get all stores", description = "Get all stores operation")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<StoreDTO>>> getAllStores(Pageable pageable) {
        Page<StoreDTO> page = storeService.getAll(pageable).map(StoreDTO::from);
        PagedModel<EntityModel<StoreDTO>> pagedModel = pagedResourcesAssembler.toModel(page,
                dto -> EntityModel.of(dto,
                        linkTo(methodOn(StoreController.class).getStore(dto.id())).withSelfRel()
                )
        );
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Delete store", description = "Delete store operation")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long storeId) {
        storeService.deleteStore(new StoreId(storeId));
        return ResponseEntity.noContent().build();
    }
}
