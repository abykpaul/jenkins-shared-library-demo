def call(int times = 3, Closure body) {
    int attempt = 0
    while (attempt < times) {
        try {
            body()
            return
        } catch (err) {
            echo "Attempt ${attempt + 1} failed: ${err}"
            attempt++
            if (attempt >= times) {
                error "All ${times} attempts failed."
            }
        }
    }
}