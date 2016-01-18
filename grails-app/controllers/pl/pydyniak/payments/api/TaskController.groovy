package pl.pydyniak.payments.api

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiImplicitParam
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.converters.JSON
import pl.pydyniak.payments.Task
import pl.pydyniak.payments.security.User

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date;

import grails.rest.RestfulController;

@Api(value = "Tasks management methods", basePath = "/api/tasks")
class TaskController extends RestfulController {
    def springSecurityService
    static allowedMethods = [addTask: 'POST', getTasks: 'GET', getTaskById: 'GET', editTask: 'PUT', deleteTask: 'DELETE']

    @ApiOperation(value = "Creating new task", httpMethod = "POST")
    @ApiResponses([
            @ApiResponse(code = 400, message = "Bad request json"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 409, message = "Conflict. Username already exists in database")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = "String",
                    defaultValue = "{'name': '', 'description':'', 'realisationDate':'', 'amount':'', 'priority':''}")
    ])
    def addTask() {
        User user = springSecurityService.getCurrentUser()
        def json = request.JSON

        if (json.isEmpty()) {
            response.status = 400
            render([Error: "Request json is empty!"] as JSON)
            return
        }
        if (json.name == null) {
            response.status = 400
            render([Error: "You need to specify name in request json"] as JSON)
            return
        }
        Task task = new Task(name: json.name, user: user)

        if (json.description != null) {
            task.description = json.description
        }
        if (json.realisationDate != null) {
            task.realisationDate = tryToParseDateOrReturnTodays(json.realisationDate)
        }
        if (json.amount != null) {
            task.amount = json.amount
        }
        if (json.priority != null) {
            task.priority = json.priority
        }


        task.timestamp = new Date().getTime()
        task.save(flush: true, failOnError: true)

        response.status = 201
        render([Status: "ok"] as JSON)
        return
    }

    private Date tryToParseDateOrReturnTodays(String date) {
        try {
            parseDate(date)
        } catch (ParseException) {
            return new Date()
        }
    }

    private Date parseDate(String date) throws ParseException{
        return new SimpleDateFormat("dd-MM-yyyy").parse(date)
    }

    @ApiOperation(value = "Returns list of all tasks of currently logged user", httpMethod = "GET")
    @ApiResponses([
            @ApiResponse(code = 405, message = "Bad method. Only GET is allowed")
    ])
    def getTasks() {
        User user = springSecurityService.getCurrentUser()

        def tasks = Task.where {
            it.user.username == user.username
        }

        def tasksList = tasks.list()
        render tasksList as JSON
    }

    @ApiOperation(value = "Returns task info by id", httpMethod = "GET")
    @ApiResponses([
            @ApiResponse(code = 204, message = "No task with such id"),
            @ApiResponse(code = 401, message = "Unauthorized. User can only see his tasks")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', paramType = 'path', required = true, dataType = "Integer")
    ])
    def getTaskById(long id) {
        User user = springSecurityService.getCurrentUser()

        def task = Task.findById(id)

        if (task == null) {
            render(status: 204)
            return
        }

        if (task.user.id != user.id) {
            render(status: 401)
            return
        }

        render task as JSON
    }

    @ApiOperation(value = "Edits currently logged user informations", httpMethod = "PUT")
    @ApiResponses([
            @ApiResponse(code = 400, message = "Bad request json"),
            @ApiResponse(code = 404, message = "No item with such id"),
            @ApiResponse(code = 401, message = "Unauthorized")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = "String",
                    defaultValue = "{\"password\":\"\"")
    ])
    def editTask(long id) {
        User user = springSecurityService.getCurrentUser()

        Task task = Task.findById(id)

        if (task == null) {
            render(status: 404)
            return
        }

        if (task.user.id != user.id) {
            render(status: 401)
            return
        }

        def json = request.JSON

        if (json.isEmpty()) {
            response.status = 400
            render(Error:"Request json is empty")
            return
        }

        if (json.name!=null) {
            task.name = json.name
        }

        if (json.description!=null) {
            task.description = json.description
        }

        if (json.realisationDate!=null) {
            task.realisationDate = tryToParseDateOrReturnTodays(json.realisationDate)
        }

        if (json.amount!=null) {
            task.amount = json.amount
        }

        if (json.priority!=null) {
            task.priority = json.priority
        }
        task.save(flush: true, failOnError: true)
        render(status: 200) // TODO check status
        return
    }

    @ApiOperation(notes = '/api/users/{id}', value = "Deletes account by id. Only for administrator", httpMethod = "DELETE")
    @ApiResponses([
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 404, message = "Task not found")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', paramType = 'path', required = true, dataType = "integer",
                    value = "Id of user to delete")
    ])
    def deleteTask(long id) {
        User user = springSecurityService.getCurrentUser()
        Task task = Task.findById(id)

        if (task == null) {
            render(status: 404)
            return
        }


        if (task.user.id != user.id) {
            render(status: 401)
            return
        }


        task.enabled = false
        task.deleted = true
        task.save(flush: true, failOnError: true)


        render(status: 200)
        return
    }

}
