package pl.pydyniak.payments

import pl.pydyniak.payments.security.User

import java.text.SimpleDateFormat
import java.util.Date;

import grails.converters.JSON;
import grails.transaction.Transactional

@Transactional
class MarshallersService {

    def register() {
        JSON.registerObjectMarshaller(Task) { Task task ->
            String d = task.realisationDate != null? new SimpleDateFormat("dd-MM-yyyy").format(task.realisationDate) : null
            return [
                    id             : task.id,
                    name           : task.name,
                    description    : task.description,
                    amount         : task.amount,
                    realisationDate: d,
                    priority       : task.priority,
                    deleted        : task.deleted,
                    enabled        : task.enabled,
                    timestamp      : task.timestamp+3600,
                    lastUpdated    : task.lastUpdated+3600
            ]
        }
        JSON.registerObjectMarshaller(User) { User user ->
            return [
                    id              : user.id,
                    username        : user.username,
            ]
        }
    }

}
