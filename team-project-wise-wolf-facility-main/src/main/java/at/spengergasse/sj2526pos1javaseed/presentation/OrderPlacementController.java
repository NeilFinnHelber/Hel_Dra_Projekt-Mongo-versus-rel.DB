package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateOrderPlacementCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Order_Placement;
import at.spengergasse.sj2526pos1javaseed.Ids.Order_PlacementId;
import at.spengergasse.sj2526pos1javaseed.dto.OrderPlacementDTO;
import at.spengergasse.sj2526pos1javaseed.services.OrderPlacementService;
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
@RequestMapping("/api/v1.0.0/order-placements")
@Validated
@Tag(name = "Order Placement", description = "Endpoints for managing order placements")
public class OrderPlacementController {

    private final OrderPlacementService orderPlacementService;
    private final PagedResourcesAssembler<OrderPlacementDTO> pagedResourcesAssembler;

    @Operation(summary = "Create order placement", description = "Create order placement operation")
    @PostMapping
    public ResponseEntity<EntityModel<OrderPlacementDTO>> createOrderPlacement(@Valid @RequestBody CreateOrderPlacementCommand cmd) {
        Order_Placement placement = orderPlacementService.create(cmd);
        OrderPlacementDTO dto = OrderPlacementDTO.from(placement);
        EntityModel<OrderPlacementDTO> entityModel = EntityModel.of(dto,
                linkTo(methodOn(OrderPlacementController.class).getOrderPlacement(dto.id())).withSelfRel(),
                linkTo(methodOn(OrderPlacementController.class).getAllOrderPlacements(null)).withRel("orderPlacements")
        );
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Get order placement", description = "Get order placement operation")
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<OrderPlacementDTO>> getOrderPlacement(@PathVariable Long id) {
        try {
            Order_Placement placement = orderPlacementService.getById(new Order_PlacementId(id));
            OrderPlacementDTO dto = OrderPlacementDTO.from(placement);
            EntityModel<OrderPlacementDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(OrderPlacementController.class).getOrderPlacement(id)).withSelfRel(),
                    linkTo(methodOn(OrderPlacementController.class).getAllOrderPlacements(null)).withRel("orderPlacements")
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get all order placements", description = "Get all order placements operation")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<OrderPlacementDTO>>> getAllOrderPlacements(Pageable pageable) {
        Page<OrderPlacementDTO> page = orderPlacementService.getAll(pageable).map(OrderPlacementDTO::from);
        PagedModel<EntityModel<OrderPlacementDTO>> pagedModel = pagedResourcesAssembler.toModel(page,
                dto -> EntityModel.of(dto,
                        linkTo(methodOn(OrderPlacementController.class).getOrderPlacement(dto.id())).withSelfRel()
                )
        );
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Delete order placement", description = "Delete order placement operation")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderPlacement(@PathVariable Long id) {
        orderPlacementService.deleteOrderPlacement(new Order_PlacementId(id));
        return ResponseEntity.noContent().build();
    }
}
