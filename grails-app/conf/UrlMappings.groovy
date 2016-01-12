class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')

        "/api/session/register"(controller: 'Session', action: 'register')
        "/api/session/edit"(controller: 'Session', action: 'edit')
        "/api/session/delete/$id"(controller:'Session', action:"delete")
        "/api/session/auth/$token"(controller: 'Session', action:'authEmail')
        "/api/session/block/$id"(controller:'Session', action: 'blockUser')

        //Tasks
        "/api/tasks"(controller: 'Task') {
            action = [GET:'getTasks', POST: 'addTask']
        }
        "/api/tasks/$id"(controller: 'Task') {
            action = [GET:'getTaskById', DELETE:'deleteTask', PUT: "editTask"]
        }


    }
}
