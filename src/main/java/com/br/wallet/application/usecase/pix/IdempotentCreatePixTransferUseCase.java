package com.br.wallet.application.usecase.pix;

import com.br.wallet.domain.model.IdempotencyRecord;
import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.port.IdempotencyRecordRepository;
import com.br.wallet.infrastructure.metrics.PixTransferMetrics;
import com.br.wallet.interfaces.rest.pix.dto.PixTransferRequest;
import com.br.wallet.interfaces.rest.pix.dto.PixTransferResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdempotentCreatePixTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(IdempotentCreatePixTransferUseCase.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final CreatePixTransferUseCase createPixTransferUseCase;
    private final ObjectMapper objectMapper;
    private final PixTransferMetrics pixTransferMetrics;
    private static final String SCOPE = "PIX_TRANSFER";

    public IdempotentCreatePixTransferUseCase(
            IdempotencyRecordRepository idempotencyRecordRepository,
            CreatePixTransferUseCase createPixTransferUseCase,
            ObjectMapper objectMapper,
            PixTransferMetrics pixTransferMetrics
    ) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.createPixTransferUseCase = createPixTransferUseCase;
        this.objectMapper = objectMapper;
        this.pixTransferMetrics = pixTransferMetrics;
    }

    @Transactional
    public PixTransferResponse execute(PixTransferRequest request, String idempotencyKey) {
        long startNs = System.nanoTime();
        String idempotencyType = "miss";
        String resultTag = "success";
        log.info(
                "IdempotentCreatePixTransferUseCase - request received fromWalletId={}, toPixKey={}, amount={}, idempotencyKey={}",
                request.fromWalletId(), request.toPixKey(), request.amount(), idempotencyKey
        );

        try {
            var existingRecord = idempotencyRecordRepository
                    .findByScopeAndIdempotencyKey(SCOPE, idempotencyKey);
            if (existingRecord.isPresent()) {
                idempotencyType = "hit";
                pixTransferMetrics.recordIdempotencyHit();
                pixTransferMetrics.recordCreateRequest(idempotencyType);

                log.info(
                        "IdempotentCreatePixTransferUseCase - idempotency hit for scope={}, idempotencyKey={}",
                        SCOPE, idempotencyKey
                );
                PixTransferResponse response = deserialize(existingRecord.get().responsePayload(), PixTransferResponse.class);
                log.info(
                        "IdempotentCreatePixTransferUseCase - returning cached response endToEndId={}, status={}",
                        response.endToEndId(), response.status()
                );

                pixTransferMetrics.recordCreateResult(idempotencyType, "success");
                return response;
            }
            pixTransferMetrics.recordIdempotencyMiss();
            pixTransferMetrics.recordCreateRequest(idempotencyType);
            PixTransfer transfer = createPixTransferUseCase.execute(
                    request.fromWalletId(),
                    request.toPixKey(),
                    request.amount(),
                    idempotencyKey
            );
            PixTransferResponse response = new PixTransferResponse(
                    transfer.endToEndId(),
                    transfer.status().name()
            );
            log.info(
                    "IdempotentCreatePixTransferUseCase - new transfer created endToEndId={}, status={}, idempotencyKey={}",
                    response.endToEndId(), response.status(), idempotencyKey
            );
            pixTransferMetrics.recordAmount(request.amount(), idempotencyType);
            persistIdempotency(idempotencyKey, response);
            pixTransferMetrics.recordCreateResult(idempotencyType, "success");
            return response;
        } catch (Exception e) {
            resultTag = "error";
            pixTransferMetrics.recordCreateResult(idempotencyType, "error");
            throw e;
        } finally {
            pixTransferMetrics.recordCreateDuration(idempotencyType, resultTag, System.nanoTime() - startNs);
        }
    }

    private void persistIdempotency(String key, Object response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            IdempotencyRecord record = IdempotencyRecord.newRecord(SCOPE, key, json);
            idempotencyRecordRepository.save(record);
            log.info(
                    "IdempotentCreatePixTransferUseCase - idempotency record persisted scope={}, idempotencyKey={}",
                    SCOPE, key
            );
        } catch (JsonProcessingException e) {
            pixTransferMetrics.recordSerializationError("serialize");
            log.error(
                    "IdempotentCreatePixTransferUseCase - error serializing idempotency response scope={}, idempotencyKey={}",
                    SCOPE, key, e
            );
            throw new IllegalStateException("Cannot serialize idempotency response", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            pixTransferMetrics.recordSerializationError("deserialize");
            log.error(
                    "IdempotentCreatePixTransferUseCase - error deserializing idempotency response scope={}, payload={}",
                    SCOPE, json, e
            );
            throw new IllegalStateException("Cannot deserialize idempotency response", e);
        }
    }
}

