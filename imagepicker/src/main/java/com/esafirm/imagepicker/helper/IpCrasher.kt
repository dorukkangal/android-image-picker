package com.esafirm.imagepicker.helper

object IpCrasher {

    fun openIssue() {
        throw IllegalStateException("This should not happen. Please open an issue!")
    }
}
