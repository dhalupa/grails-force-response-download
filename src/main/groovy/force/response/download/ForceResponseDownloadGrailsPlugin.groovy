package force.response.download

import grails.plugins.*

class ForceResponseDownloadGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.1 > *"
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "web-app/**",
            "test/**"
    ]

    def author = "Predrag Knezevic"
    def authorEmail = "pedjak@gmail.com"
    def title = "Force Downloading Controller's Response in Browser"

    def license = "APACHE"

    def scm = [url: "http://github.com/pedjak/grails-force-response-download"]

    def description = '''\\
The plugin forces browser to open a dialog for downloading content produced within controller's action.
Although the theory says that this is easily controlled by specifying Content-Disposition HTTP header, the practice shows
that there are special situations (ofcourse) with IE that has to be handled properly.

Controllers are extended with forceDownload method that takes as parameters a Map specifying download options,
and a object containing content:

    forceDownload(filename:"file", contentType:"application/octet-stream", contentLength: 123, content)

* filename specifies the name that will be presented in browser download dialog, if omitted the default value is 'file'
* contentType is MIME content type that will be sent to browser for the given content, if omitted the default value is application/octet-stream
* contentLength is optional, but recommended to have - browsers will be able to show proper progress while downloading.
  If not explicitely specified, it can be read from content object, if it implements size() method
* content - optional. If omitted then controller's code must write to response stream or render response manually using standard Grails approaches

The source code is available at http://github.com/pedjak/grails-force-response-download
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/force-file-download"

    def observe = ['controllers']

    Closure doWithSpring() {
        { ->
            // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        configureControllers(grailsApplication)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        if (event.source && grailsApplication.isControllerClass(event.source.class)) {
            def context = event.ctx
            if (!context) {
                return
            }
            configureControllers(grailsApplication)
        }
    }

    private def configureControllers(application) {
        def service = application.mainContext.userAgentIdentService
        application.controllerClasses*.clazz.each { cls ->
            cls.metaClass.forceDownload = { Map map = [filename: "file", contentType: "application/octet-stream"], content = null ->
                def response = delegate.response
                def isIE = service.isMsie()

                if (delegate.request.isSecure()) {
                    response.addHeader("Pragma", "no-cache")
                    response.addHeader("Expires", "-1")
                    response.addHeader("Cache-Control", "no-cache")
                } else {
                    response.addHeader("Cache-Control", "private")
                    response.addHeader("Pragma", "public")
                }
                response.addHeader("Content-Disposition", "attachment; filename=\"${map.filename}\"");
                if (isIE) {
                    response.addHeader("Connection", "close");
                }
                response.contentType = map.contentType
                def length = map.contentLength ?: content != null ? (content.metaClass.respondsTo(content, "size") ? content.size() : null) : null

                if (length != null) {
                    response.addHeader("Content-Length", "${length}");
                }
                def os = response.outputStream
                if (content != null) {
                    os << content
                    os.close()
                }
                os
            }
        }
    }

}
