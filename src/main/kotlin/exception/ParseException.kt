package exception

class ParseException(message: String?, cause: Throwable?, enableSuppression: Boolean = false, writableStackTrace: Boolean = true)
    : RuntimeException(message, cause, enableSuppression, writableStackTrace)