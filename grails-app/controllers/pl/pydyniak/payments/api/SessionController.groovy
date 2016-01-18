package pl.pydyniak.payments.api

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiImplicitParam
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiOperation
import com.wordnik.swagger.annotations.ApiResponse
import com.wordnik.swagger.annotations.ApiResponses
import grails.converters.JSON
import grails.rest.RestfulController
import grails.transaction.Transactional
import org.apache.commons.lang.RandomStringUtils
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import pl.pydyniak.payments.security.ConfirmationToken
import pl.pydyniak.payments.security.Role
import pl.pydyniak.payments.security.User
import pl.pydyniak.payments.security.UserRole

@Api(value = "Methods for managing users", basePath = "/api/users", consumes = "application/json")
class SessionController extends RestfulController {
    def springSecurityService
    def mailService


        static allowedMethods = [register: 'POST', getUsers: 'GET', getUserById:'GET',
                                 edit: 'PUT', delete: 'DELETE', deleteById: 'DELETE', authEmail: 'GET']

    String tokenCharset = (('A'..'Z') + ('0'..'9')).join()
    Integer tokenLength = 32

    static responseFormats = ['json']


    @ApiOperation(value = "Creating new user", httpMethod = "POST")
    @ApiResponses([
            @ApiResponse(code = 400, message = "Bad request json"),
            @ApiResponse(code = 409, message = "Conflict. Username already exists in database")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = "String",
            defaultValue = "{'username': '', 'password':''}")
    ])
    @Transactional
    def register(User user) {
        println "Register method"

        def json = request.JSON

        if (user.username == null) {
            response.status = 400
            render([Error: "Username can't be null!"] as JSON)
            return
        }
        if (user.password == null) {
            response.status = 400
            render([Error: "Password can't be null!"] as JSON)
            return
        }

        if (User.findByUsername(user.username) != null) {
            render(status: 409)
            return
        }

        if (user.password.size() < 6) {
            respond status: "Password has to have at least 6 characters"
            return
        }

        String randomToken = RandomStringUtils.random(tokenLength, tokenCharset.toCharArray())
        if (ConfirmationToken.findByToken(randomToken) != null) {
            while (ConfirmationToken.findByToken(randomToken) != null) {
                randomToken = RandomStringUtils.random(tokenLength, tokenCharset.toCharArray())
            }
        }
        user.save(flush: true, failOnError: true)
        def role = Role.findByAuthority("ROLE_USER")


        UserRole.create user, role, true

        new ConfirmationToken(
                token: randomToken,
                user: user
        ).save(flush: true, failOnError: true)

//        sendConfirmationEmail(user, randomToken)
        user.enabled = true
        user.save(flush:true)
        render(status: 201)
        return

    }

    @ApiOperation(value = "Returns list of all users", httpMethod = "GET")
    def getUsers() {
        render User.findAll() as JSON
    }

    @ApiOperation(value = "Returns user info by id", httpMethod = "GET")
    @ApiResponses([
            @ApiResponse(code = 204, message = "No user with such id")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', paramType = 'path', required = true, dataType = "Integer")
            ])
    def getUserById(int id) {
        User user = User.findById(id)

        if (user == null || !user.enabled) {
            response.status = 204
            render ([Error:"No such user"] as JSON)
            return
        }

        render user as JSON
    }

    @ApiOperation(value = "Edits currently logged user informations", httpMethod = "PUT")
    @ApiResponses([
            @ApiResponse(code = 400, message = "Bad request json"),
            @ApiResponse(code = 401, message = "Unauthorized")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = "String",
            defaultValue = "{\"password\":\"\"")
    ])
    @Transactional
    def edit() {
        User currentUser = springSecurityService.currentUser

        def json = request.JSON

        if (json.isEmpty()) {
            render(status: 400)
            return
        }

        if (json.password != null) {
            currentUser.password = json.password
        }

        currentUser.save(flush: true, failOnError: true)

        respond status: "OK"
        return
    }

    private def sendConfirmationEmail(User user, String token) {
        def serverUrl = grailsApplication.config.serverURL
        def authenticateResourceUrl = "api/session/auth/"
        def emailBody = "Go to the site to authenticate your account " + serverUrl + authenticateResourceUrl
        def emailSubject = serverUrl + " account verification"

        System.out.println("Sending confirmation email to " + user.username)
        emailBody + token
        mailService.sendMail {
            to(user.username)
            subject emailSubject
            body(emailBody + token)
        }

        println "Confirmation email sent"
    }


    @ApiOperation(notes = '/api/users/auth/$token', value = "Method to verificate email", httpMethod = "GET")
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'token', value = "Token that is sent on user's email",
                    paramType = 'path', required = true, dataType = "String")
    ])
    @Transactional
    def authEmail(String token) {
        def confirmationToken = ConfirmationToken.findByToken(token)

        if (confirmationToken == null) {
            respond status: "Invalid token!"
            return
        }

        if (confirmationToken.activated == true) {
            respond status: "Token has been already used!"
            return
        }

        confirmationToken.user.enabled = true
        confirmationToken.user.save(flush: true, failOnError: true)

        confirmationToken.activated = true
        confirmationToken.save(flush: true, failOnError: true)

        respond status: "User has been activated"
        return
    }

    @ApiOperation(notes = '/api/users', value = "Deletes currently logged user", httpMethod = "DELETE")
    @ApiResponses([
            @ApiResponse(code = 401, message = "Unauthorized")
    ])
    def delete() {
        User user = springSecurityService.getCurrentUser()
        deleteUser(user)
    }

    private void deleteUser(User user) {
        user.enabled = false
        user.accountLocked = true
        user.save(flush:true)
    }

    @ApiOperation(notes = '/api/users', value = "Deletes account by id. Only for administrator", httpMethod = "DELETE")
    @ApiResponses([
            @ApiResponse(code = 401, message = "Unauthorized. Method only for administrator"),
            @ApiResponse(code = 204, message = "User not found")
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', paramType = 'path', required = true, dataType = "integer",
            value = "Id of user to delete")
    ])
    @Transactional
    def deleteById(int id) {
        User user = User.findById(id)

        if (user == null) {
            response.status = 204
            render([Error:"No such user"] as JSON)
            return
        }

        deleteUser(user)
    }
}
