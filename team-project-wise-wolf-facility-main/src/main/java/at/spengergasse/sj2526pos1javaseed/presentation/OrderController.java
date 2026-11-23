package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateOrderCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.Order;
import at.spengergasse.sj2526pos1javaseed.Ids.OrderId;
import at.spengergasse.sj2526pos1javaseed.dto.OrderDTO;
import at.spengergasse.sj2526pos1javaseed.services.OrderService;
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
@RequestMapping("/api/v1.0.0/orders")
@Validated
@Tag(name = "Order", description = "Endpoints for managing orders")
public class OrderController {

    private final OrderService orderService;
    private final PagedResourcesAssembler<OrderDTO> pagedResourcesAssembler;

    // neue bestellung erstellen und als hateoas-model zurückgeben
    @Operation(summary = "Create order", description = "Create order operation")
    @PostMapping
    public ResponseEntity<EntityModel<OrderDTO>> createOrder(@Valid @RequestBody CreateOrderCommand cmd) {
        Order order = orderService.create(cmd);
        OrderDTO dto = OrderDTO.from(order);
        EntityModel<OrderDTO> entityModel = EntityModel.of(dto,
                linkTo(methodOn(OrderController.class).getOrder(dto.id())).withSelfRel(),
                linkTo(methodOn(OrderController.class).getAllOrders(null)).withRel("orders")
        );
        return ResponseEntity.ok(entityModel);
    }

    // eine einzelne bestellung anhand der id laden
    @Operation(summary = "Get order", description = "Get order operation")
    @GetMapping("/{orderId}")
    public ResponseEntity<EntityModel<OrderDTO>> getOrder(@PathVariable Long orderId) {
        try {
            Order order = orderService.getById(new OrderId(orderId));
            OrderDTO dto = OrderDTO.from(order);
            EntityModel<OrderDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(OrderController.class).getOrder(orderId)).withSelfRel(),
                    linkTo(methodOn(OrderController.class).getAllOrders(null)).withRel("orders")
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // alle bestellungen seitenweise laden
    @Operation(summary = "Get all orders", description = "Get all orders operation")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<OrderDTO>>> getAllOrders(Pageable pageable) {
        Page<OrderDTO> page = orderService.getAll(pageable).map(OrderDTO::from);
        PagedModel<EntityModel<OrderDTO>> pagedModel = pagedResourcesAssembler.toModel(page,
                dto -> EntityModel.of(dto,
                        linkTo(methodOn(OrderController.class).getOrder(dto.id())).withSelfRel()
                )
        );
        return ResponseEntity.ok(pagedModel);
    }

    // bestellung löschen (cascade entfernt auch die zugehörige bestellplatzierung)
    @Operation(summary = "Delete order", description = "Delete order operation")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(new OrderId(orderId));
        return ResponseEntity.noContent().build();
    }
}
