package com.csuft.oj.controller;

import com.csuft.oj.service.RanklistService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.UserRankVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User ranklist APIs.
 */
@RestController
public class UserRankController {

    private final RanklistService ranklistService;

    public UserRankController(RanklistService ranklistService) {
        this.ranklistService = ranklistService;
    }

    /**
     * Global ranklist ordered by solved count descending and submit count ascending.
     */
    @GetMapping("/api/users/ranklist")
    public ApiResponse<PageResult<UserRankVO>> ranklist(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        return ApiResponse.success(ranklistService.ranklist(page, size));
    }
}
