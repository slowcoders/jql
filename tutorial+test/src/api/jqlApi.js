import axios from "axios";

const baseUrl = 'http://localhost:7007/api/jql/starwars/character';

const default_options = {
    select: "*",
    sort: '',
    limit: 0,
    page: 0,
}

function to_url_param(options) {
    if (!options) return "";

    let params = ""
    for (const k in options) {
        params += params.length > 1 ? '&' : '?';
        params += k + "=" + options[k];
    }
    return params;
}

const http_options = {
    headers: {
        "Content-Type" : "application/json"
    }
}
async function call_http(method, command, filter, options) {
    //const params = to_url_param(options)
    const url = `${baseUrl}/${command}${to_url_param(options)}`
    const response = await axios[method].call(axios, url, filter, http_options);
    return response.data;
}


export const jqlApi = {
    cachedListTs: 0,
    cachedList: null,

    async count(filter) {
        return await call_http('post', 'count', filter)
    },

    async find(filter, options) {
        return await call_http('post', 'find', filter, options);
    },

    async top(filter, options) {
        options = { ...options, page: -1, limit: 1 }
        const res = await call_http('post', 'find', filter, options);
        return res.content.length > 0 ? res.content[0] : null;
    },
}

