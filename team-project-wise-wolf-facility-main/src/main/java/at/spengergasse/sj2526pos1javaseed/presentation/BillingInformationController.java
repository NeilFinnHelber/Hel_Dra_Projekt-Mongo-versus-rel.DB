package at.spengergasse.sj2526pos1javaseed.presentation;

import at.spengergasse.sj2526pos1javaseed.Commands.CreateBillingInformationCommand;
import at.spengergasse.sj2526pos1javaseed.Domain.BillingInformation;
import at.spengergasse.sj2526pos1javaseed.Ids.BillingInformationId;
import at.spengergasse.sj2526pos1javaseed.dto.BillingInformationDTO;
import at.spengergasse.sj2526pos1javaseed.services.BillingInformationService;
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
@RequestMapping("/api/v1.0.0/billing")
@Validated
@Tag(name = "Billing Information", description = "Endpoints for managing billing informations")
public class BillingInformationController {

    private final BillingInformationService billingInformationService;
    private final PagedResourcesAssembler<BillingInformationDTO> pagedResourcesAssembler;

    @Operation(summary = "Create billing", description = "Create billing operation")
    @PostMapping
    public ResponseEntity<EntityModel<BillingInformationDTO>> createBilling(@Valid @RequestBody CreateBillingInformationCommand cmd) {
        BillingInformation bi = billingInformationService.create(cmd);
        BillingInformationDTO dto = BillingInformationDTO.from(bi);
        EntityModel<BillingInformationDTO> entityModel = EntityModel.of(dto,
                linkTo(methodOn(BillingInformationController.class).getBilling(dto.id())).withSelfRel(),
                linkTo(methodOn(BillingInformationController.class).getAllBilling(null)).withRel("billing")
        );
        return ResponseEntity.ok(entityModel);
    }

    @Operation(summary = "Get billing", description = "Get billing operation")
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<BillingInformationDTO>> getBilling(@PathVariable Long id) {
        try {
            BillingInformation bi = billingInformationService.getById(new BillingInformationId(id));
            BillingInformationDTO dto = BillingInformationDTO.from(bi);
            EntityModel<BillingInformationDTO> entityModel = EntityModel.of(dto,
                    linkTo(methodOn(BillingInformationController.class).getBilling(id)).withSelfRel(),
                    linkTo(methodOn(BillingInformationController.class).getAllBilling(null)).withRel("billing")
            );
            return ResponseEntity.ok(entityModel);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get all billing", description = "Get all billing operation")
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<BillingInformationDTO>>> getAllBilling(Pageable pageable) {
        Page<BillingInformationDTO> page = billingInformationService.getAll(pageable).map(BillingInformationDTO::from);
        PagedModel<EntityModel<BillingInformationDTO>> pagedModel = pagedResourcesAssembler.toModel(page,
                dto -> EntityModel.of(dto,
                        linkTo(methodOn(BillingInformationController.class).getBilling(dto.id())).withSelfRel()
                )
        );
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(summary = "Delete billing", description = "Delete billing operation")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBilling(@PathVariable Long id) {
        billingInformationService.deleteBillingInformation(new BillingInformationId(id));
        return ResponseEntity.noContent().build();
    }
}
