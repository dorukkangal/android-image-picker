package com.esafirm.imagepicker.features.common

open class BasePresenter<T : MvpView> {
    private var view: T? = null

    protected val isViewAttached: Boolean
        get() = view != null

    fun attachView(view: T) {
        this.view = view
    }

    fun detachView() {
        view = null
    }

    fun getView(): T {
        return if (isViewAttached) {
            view!!
        } else {
            throw RuntimeException("View is detached")
        }
    }
}
