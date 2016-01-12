package pl.pydyniak.payments

import java.util.Date;

import grails.converters.JSON;
import grails.transaction.Transactional

@Transactional
class MarshallersService {

    def register() {
        JSON.registerObjectMarshaller(Task) { Task task ->
            return [
                    id             : task.id,
                    name           : task.name,
                    description    : task.description,
                    amount         : task.amount,
                    realisationDate: task.realisationDate,
                    priority       : task.priority,
                    deleted        : task.deleted,
                    enabled        : task.enabled
            ]
        }
    }

}
