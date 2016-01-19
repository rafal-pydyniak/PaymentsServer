class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')

        "/api/docs" (controller: "api") //Api docs

        //Users
        "/api/users"(controller: 'Users') {
            action = [GET: 'getUsers', POST: 'register', PUT: 'edit', DELETE:'delete']
        }

        "/api/users/$id"(controller: 'Users') {
            action = [DELETE: 'deleteById', GET: 'getUserById']
        }
        "/api/users/auth/$token"(controller: 'Session', action:'authEmail')

        //Tasks
        "/api/tasks"(controller: 'Tasks') {
            action = [GET:'getTasks', POST: 'addTask']
        }
        "/api/tasks/$id"(controller: 'Tasks') {
            action = [GET:'getTaskById', DELETE:'deleteTask', PUT: "editTask"]
        }


    }
}
