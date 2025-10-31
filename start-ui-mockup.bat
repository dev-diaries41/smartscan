@echo off
echo ========================================
echo   SmartScan UI Mockup Dev Server
echo ========================================
echo.
echo Starting development server...
echo.

cd /D C:\Users\jaroslav\.claude\plugins\marketplaces\anthropic-agent-skills\artifacts-builder\smartscan-ui-mockup

echo Installing dependencies (if needed)...
call pnpm install

echo.
echo Starting dev server...
echo.
echo Mockup will be available at: http://localhost:5173
echo.
echo Press Ctrl+C to stop the server
echo ========================================
echo.

call pnpm run dev

pause
