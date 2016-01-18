class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')

        //Users
        "/api/users"(controller: 'Session') {
            action = [GET: 'getUsers', POST: 'register', PUT: 'EDIT', DELETE:'delete']
        }

        "/api/users/$id"(controller: 'Session') {
            action = [DELETE: 'deleteById', GET: 'getById']
        }
        "/api/users/auth/$token"(controller: 'Session', action:'authEmail')

        //Tasks
        "/api/tasks"(controller: 'Task') {
            action = [GET:'getTasks', POST: 'addTask']
        }
        "/api/tasks/$id"(controller: 'Task') {
            action = [GET:'getTaskById', DELETE:'deleteTask', PUT: "editTask"]
        }


    }
}
