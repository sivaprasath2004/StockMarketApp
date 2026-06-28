#!/bin/bash
# ic_market.xml
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_market.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="@color/text_secondary">
  <path android:fillColor="@android:color/white"
      android:pathData="M3.5,18.49l6,-6.01 4,4L22,6.92l-1.41,-1.41 -7.09,7.97 -4,-4L2,16.99z"/>
</vector>
EOF

# ic_portfolio.xml
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_portfolio.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="@color/text_secondary">
  <path android:fillColor="@android:color/white"
      android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13.41,18.09L13.41,20h-2v-1.93c-1.71,-0.36 -3.16,-1.46 -3.27,-3.4h1.96c0.1,1.05 0.82,1.87 2.65,1.87 1.96,0 2.4,-0.98 2.4,-1.59 0,-0.83 -0.44,-1.61 -2.67,-2.14 -2.48,-0.6 -4.18,-1.62 -4.18,-3.67 0,-1.72 1.39,-2.84 3.11,-3.21L11.41,4h2v1.95c1.86,0.45 2.79,1.86 2.85,3.39L14.3,9.34c-0.05,-1.11 -0.64,-1.87 -2.22,-1.87 -1.5,0 -2.4,0.68 -2.4,1.64 0,0.84 0.65,1.39 2.67,1.91s4.18,1.39 4.18,3.91c-0.01,1.83 -1.38,2.83 -3.12,3.16z"/>
</vector>
EOF

# ic_watchlist.xml
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_watchlist.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="@color/text_secondary">
  <path android:fillColor="@android:color/white"
      android:pathData="M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"/>
</vector>
EOF

# ic_settings.xml
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_settings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="@color/text_secondary">
  <path android:fillColor="@android:color/white"
      android:pathData="M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94 0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6 0,-1.98 1.62,-3.6 3.6,-3.6 1.98,0 3.6,1.62 3.6,3.6 0,1.98 -1.62,3.6 -3.6,3.6z"/>
</vector>
EOF

# ic_refresh.xml
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_refresh.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="@color/white">
  <path android:fillColor="@android:color/white"
      android:pathData="M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z"/>
</vector>
EOF

# ic_add.xml
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_add.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="@color/white">
  <path android:fillColor="@android:color/white"
      android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
</vector>
EOF

# bg_signal.xml  (rounded corner background for signal text)
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/bg_signal.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <corners android:radius="6dp" />
</shape>
EOF

# ic_launcher placeholder (simple circle)
cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_launcher_background.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#0D1B2A" />
</shape>
EOF

cat > /home/claude/StockMarketApp/app/src/main/res/drawable/ic_launcher_foreground.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
  <path android:fillColor="#00B4D8"
      android:pathData="M54,20 L80,50 L60,50 L60,88 L48,88 L48,50 L28,50 Z"/>
</vector>
EOF

echo "All drawables created successfully"
