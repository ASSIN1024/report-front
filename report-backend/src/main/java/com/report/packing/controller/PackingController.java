package com.report.packing.controller;

import com.report.packing.manager.PackingManager;
import com.report.packing.service.PackingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/packing")
public class PackingController {

    @Autowired
    private PackingConfigService packingConfigService;

    @Autowired
    private PackingManager packingManager;

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return packingConfigService.getAllConfigs();
    }

    @PutMapping("/config")
    public void updateConfig(@RequestParam String key, @RequestParam String value) {
        packingConfigService.updateConfig(key, value);
    }

    @PostMapping("/trigger")
    public String triggerPacking() {
        packingManager.executePacking();
        return "Packing triggered";
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("canUpload", packingManager.checkAndWaitForConsumption());
        result.put("uploadDir", packingConfigService.getStringValue("upload_dir", "/data/ftp-root/for-upload"));
        result.put("doneDir", packingConfigService.getStringValue("done_dir", "/data/ftp-root/done"));
        result.put("fixedFilename", packingConfigService.getStringValue("fixed_filename", "outputs.zip"));
        return result;
    }
}