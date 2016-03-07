package pl.pydyniak.payments.api

import grails.converters.JSON
import pl.pydyniak.payments.Task
import pl.pydyniak.payments.security.User

import java.util.Date;

import grails.rest.RestfulController;

class TaskController extends RestfulController {
    def springSecurityService
    static allowedMethods = [addTask: 'POST', getTasks: 'GET', getTaskById: 'GET', editTask: 'PUT', deleteTask: 'DELETE']

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
            Date date = new Date(json.realisationDate)
            task.realisationDate = date
        }
        if (json.amount != null) {
            task.amount = json.amount
        }
        if (json.priority != null) {
            task.priority = json.priority
        }


        task.save(flush: true, failOnError: true)

        response.status = 201
        render([Status: "ok"] as JSON)
        return
    }

    def getTasks() {
        User user = springSecurityService.getCurrentUser()

        def tasks = Task.where {
            it.user.username == user.username
        }

        def tasksList = tasks.list()
        render tasksList as JSON
    }

    def getTaskById(long id) {
        User user = springSecurityService.getCurrentUser()

        def task = Task.findById(id)

        if (task.user.id != user.id) {
            render(status: 401) //TODO check status
            return
        }

        render task as JSON
    }

    def editTask(long id) {
        User user = springSecurityService.getCurrentUser()

        Task task = Task.findById(id)

        if (task == null) {
            render(status: 400)
            return
        }

        if (task.user.id != user.id) {
            render(status: 302)
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
            task.realisationDate = new Date(json.realisationDate)
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

    def deleteTask(long id) {
        User user = springSecurityService.getCurrentUser()
        Task task = Task.findById(id)

        if (task == null) {
            render(status: 400) //TODO
            return
        }


        if (task.user.id != user.id) {
            render(status: 302) //TODO check status
            return
        }


        task.enabled = false
        task.deleted = true
        task.save(flush: true, failOnError: true)


        render(status: 200) //TODO
        return
    }

}
