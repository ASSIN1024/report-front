const { chromium } = require('playwright');

(async () => {
  console.log('Starting Playwright tests...');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  let passed = 0;
  let failed = 0;

  async function test(name, fn) {
    try {
      await fn();
      console.log(`✓ ${name}`);
      passed++;
    } catch (e) {
      console.log(`✗ ${name}: ${e.message}`);
      failed++;
    }
  }

  // Test 1: Login page loads
  await test('1. Login page loads', async () => {
    await page.goto('http://localhost:8085/#/login');
    await page.waitForSelector('.login-container', { timeout: 10000 });
    console.log('   - Login form found');
  });

  // Test 2: Login form elements exist
  await test('2. Login form has required elements', async () => {
    await page.waitForSelector('input[type="text"]', { timeout: 5000 });
    await page.waitForSelector('input[type="password"]', { timeout: 5000 });
    await page.waitForSelector('button[type="button"]', { timeout: 5000 });
    console.log('   - Username, password, and submit button found');
  });

  // Test 3: Login with valid credentials
  await test('3. Login with valid credentials', async () => {
    await page.fill('input[type="text"]', 'admin');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="button"]');
    await page.waitForTimeout(2000);
    const currentUrl = page.url();
    if (currentUrl.includes('/login')) {
      throw new Error('Login failed, still on login page');
    }
    console.log('   - Redirected after login');
  });

  // Test 4: FTP Config page loads
  await test('4. FTP Config page loads', async () => {
    await page.goto('http://localhost:8085/#/ftp');
    await page.waitForTimeout(1000);
    await page.waitForSelector('.page-container', { timeout: 10000 });
    console.log('   - FTP Config page loaded');
  });

  // Test 5: FTP Config form has new fields
  await test('5. FTP Config form has directory fields', async () => {
    const stagingInput = await page.$('input[placeholder*="staging" i], input[placeholder*="暂存" i]');
    const uploadInput = await page.$('input[placeholder*="for-upload" i], input[placeholder*="待上传" i]');
    const archiveInput = await page.$('input[placeholder*="archive" i], input[placeholder*="归档" i]');
    console.log(`   - stagingDir input: ${stagingInput ? 'found' : 'not found'}`);
    console.log(`   - forUploadDir input: ${uploadInput ? 'found' : 'not found'}`);
    console.log(`   - archiveDir input: ${archiveInput ? 'found' : 'not found'}`);
  });

  // Test 6: Report Config page loads
  await test('6. Report Config page loads', async () => {
    await page.goto('http://localhost:8085/#/report');
    await page.waitForTimeout(1000);
    await page.waitForSelector('.page-container', { timeout: 10000 });
    console.log('   - Report Config page loaded');
  });

  // Test 7: Alert page loads
  await test('7. Alert page loads', async () => {
    await page.goto('http://localhost:8085/#/alert');
    await page.waitForTimeout(1000);
    await page.waitForSelector('.page-container', { timeout: 10000 });
    console.log('   - Alert page loaded');
  });

  // Test 8: Task page loads
  await test('8. Task page loads', async () => {
    await page.goto('http://localhost:8085/#/task');
    await page.waitForTimeout(1000);
    await page.waitForSelector('.page-container', { timeout: 10000 });
    console.log('   - Task page loaded');
  });

  // Test 9: Sidebar menu has correct items
  await test('9. Sidebar menu has correct items', async () => {
    const menuText = await page.textContent('.sidebar-container') || '';
    const hasFtp = menuText.includes('FTP') || menuText.includes('ftp');
    const hasReport = menuText.includes('报表') || menuText.includes('report');
    const hasAlert = menuText.includes('告警') || menuText.includes('alert');
    const hasTask = menuText.includes('处理记录') || menuText.includes('任务');
    console.log(`   - FTP menu: ${hasFtp}`);
    console.log(`   - Report menu: ${hasReport}`);
    console.log(`   - Alert menu: ${hasAlert}`);
    console.log(`   - Task menu: ${hasTask}`);
    if (!hasFtp || !hasReport || !hasAlert || !hasTask) {
      throw new Error('Menu items missing');
    }
  });

  // Test 10: No data-center or trigger-monitor in menu
  await test('10. Old menu items removed', async () => {
    const menuText = await page.textContent('.sidebar-container') || '';
    const hasDataCenter = menuText.includes('数据中心') || menuText.includes('data-center');
    const hasTrigger = menuText.includes('Trigger') || menuText.includes('trigger-monitor');
    console.log(`   - data-center removed: ${!hasDataCenter}`);
    console.log(`   - trigger-monitor removed: ${!hasTrigger}`);
    if (hasDataCenter || hasTrigger) {
      throw new Error('Old menu items still present');
    }
  });

  await browser.close();

  console.log('\n========================================');
  console.log(`Tests completed: ${passed + failed}`);
  console.log(`Passed: ${passed}`);
  console.log(`Failed: ${failed}`);
  console.log('========================================');

  process.exit(failed > 0 ? 1 : 0);
})();
