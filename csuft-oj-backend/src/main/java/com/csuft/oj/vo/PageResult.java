package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paged response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * Current page number, starting from 1.
     */
    private Long page;

    /**
     * Page size.
     */
    private Long size;

    /**
     * Total record count.
     */
    private Long total;

    /**
     * Total page count.
     */
    private Long pages;

    /**
     * Records on the current page.
     */
    private List<T> records;
}
