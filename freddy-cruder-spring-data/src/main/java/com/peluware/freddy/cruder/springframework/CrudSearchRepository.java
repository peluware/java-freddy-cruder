package com.peluware.freddy.cruder.springframework;

import com.peluware.omnisearch.EntityOmniSearch;
import org.springframework.data.repository.CrudRepository;

public interface CrudSearchRepository<ENTITY, ID> extends CrudRepository<ENTITY, ID>, EntityOmniSearch<ENTITY> {
}
