# Simple Mock Server using PowerShell
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:8080/")
$listener.Start()

Write-Host "🚀 Mock Server started on http://localhost:8080"
Write-Host "📡 Available endpoints:"
Write-Host "   GET /api/bins - Get all bins"
Write-Host "   GET /api/bins/nearby - Get nearby bins"
Write-Host "Press Ctrl+C to stop"

# Sample bin data
$sampleBins = @"
[
    {
        "binId": 1,
        "binCode": "BIN001",
        "latitude": 21.0285,
        "longitude": 105.8542,
        "status": "ACTIVE",
        "currentFill": 75.5,
        "street": "Phố Huế, Hai Bà Trưng, Hà Nội"
    },
    {
        "binId": 2,
        "binCode": "BIN002", 
        "latitude": 21.0295,
        "longitude": 105.8552,
        "status": "ACTIVE",
        "currentFill": 45.2,
        "street": "Phố Lê Duẩn, Hai Bà Trưng, Hà Nội"
    },
    {
        "binId": 3,
        "binCode": "BIN003",
        "latitude": 21.0305,
        "longitude": 105.8562,
        "status": "INACTIVE",
        "currentFill": 90.0,
        "street": "Phố Trần Hưng Đạo, Hoàn Kiếm, Hà Nội"
    }
]
"@

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response
        
        Write-Host "📥 Request: $($request.HttpMethod) $($request.Url.AbsolutePath)"
        
        # Set CORS headers
        $response.Headers.Add("Access-Control-Allow-Origin", "*")
        $response.Headers.Add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        $response.Headers.Add("Access-Control-Allow-Headers", "Content-Type, Authorization")
        
        if ($request.HttpMethod -eq "OPTIONS") {
            $response.StatusCode = 200
            $response.Close()
            continue
        }
        
        # Handle different endpoints
        switch ($request.Url.AbsolutePath) {
            "/api/bins" {
                $response.ContentType = "application/json"
                $response.StatusCode = 200
                $buffer = [System.Text.Encoding]::UTF8.GetBytes($sampleBins)
                $response.ContentLength64 = $buffer.Length
                $response.OutputStream.Write($buffer, 0, $buffer.Length)
                Write-Host "✅ Response: 200 OK - Bins data"
            }
            "/api/bins/nearby" {
                $response.ContentType = "application/json"
                $response.StatusCode = 200
                $buffer = [System.Text.Encoding]::UTF8.GetBytes($sampleBins)
                $response.ContentLength64 = $buffer.Length
                $response.OutputStream.Write($buffer, 0, $buffer.Length)
                Write-Host "✅ Response: 200 OK - Nearby bins data"
            }
            default {
                $response.StatusCode = 404
                $response.Close()
                Write-Host "❌ Response: 404 Not Found"
            }
        }
        
        $response.Close()
    }
}
finally {
    $listener.Stop()
    Write-Host "🛑 Mock Server stopped"
}


