
interface Event {
    id: string,
    start: string,
    end: string,
    current: boolean,
    sponsors: Array<any>
}

export default class EventLoader {
    loadEvent(id) : Promise<Response>{
        return fetch(`/api/event/${id}`, {method: 'get'});
    }
}