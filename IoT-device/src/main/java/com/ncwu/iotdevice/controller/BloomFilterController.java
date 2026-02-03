package com.ncwu.iotdevice.controller;


import com.ncwu.common.apis.BloomFilterService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */

@RestController
@RequestMapping("/bloomFilter")
@RequiredArgsConstructor
public class BloomFilterController {

    private final BloomFilterService bloomFilterService;

    @PostMapping("/contains")
    public boolean mightContain(@RequestBody List<@NotBlank String> ids){
       return bloomFilterService.mightContains(ids);
    }

    @PostMapping("/add")
    public boolean add(@RequestBody List<@NotBlank String> ids){
        return bloomFilterService.add(ids);
    }
}
