
import { test } from '@playwright/test';
import { expect } from '@playwright/test';

test('PackingModule_2026-04-28', async ({ page, context }) => {
  
    // Navigate to URL
    await page.goto('http://localhost:8091/packing/config');

    // Take screenshot
    await page.screenshot({ path: 'packing_config_page.png', { fullPage: true } });

    // Navigate to URL
    await page.goto('http://localhost:8091/login');

    // Take screenshot
    await page.screenshot({ path: 'login_page.png', { fullPage: true } });

    // Fill input field
    await page.fill('input[type="text"], input[placeholder*="用户名"], input[placeholder*="账号"]', 'admin');

    // Fill input field
    await page.fill('input[type="password"]', 'admin123');

    // Click element
    await page.click('button[type="submit"], button:has-text("登录")');

    // Take screenshot
    await page.screenshot({ path: 'after_login_fill.png' });

    // Navigate to URL
    await page.goto('http://localhost:8091/packing/config');

    // Take screenshot
    await page.screenshot({ path: 'packing_config_after_login.png', { fullPage: true } });

    // Navigate to URL
    await page.goto('http://localhost:8091/#/packing/config');

    // Take screenshot
    await page.screenshot({ path: 'packing_config_hash.png', { fullPage: true } });

    // Take screenshot
    await page.screenshot({ path: 'packing_config_full.png', { fullPage: true } });
});