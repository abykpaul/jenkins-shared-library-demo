def call(Closure body) {
    retry(2) {
        try {
            body()
        } catch (e) {
            echo "Retry failed: ${e.message}"
            throw e
        }
    }
}