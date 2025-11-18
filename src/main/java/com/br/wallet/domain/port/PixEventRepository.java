package com.br.wallet.domain.port;

import com.br.wallet.domain.model.PixEvent;

import java.util.Optional;

public interface PixEventRepository {
    PixEvent save(PixEvent event);
    Optional<PixEvent> findByEventId(String eventId);
}
