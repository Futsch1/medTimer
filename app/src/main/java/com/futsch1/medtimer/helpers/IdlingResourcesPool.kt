package com.futsch1.medtimer.helpers

class IdlingResourcesPool private constructor() {
    var resources: MutableMap<String, SimpleIdlingResource> = HashMap()

    companion object {
        private var instance: IdlingResourcesPool? = null

        @JvmStatic
        fun getInstance(): IdlingResourcesPool {
            if (instance == null) {
                instance = IdlingResourcesPool()
            }
            return instance!!
        }
    }

    fun getResource(name: String): SimpleIdlingResource {
        return if (resources.containsKey(name)) {
            resources.getValue(name)
        } else {
            val resource = SimpleIdlingResource(name)
            resources[name] = resource
            resource
        }
    }


}