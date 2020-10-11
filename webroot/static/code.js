const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');



const saveButton = document.querySelector('#post-service');

saveButton.onclick = evt => {
    let urlName = document.querySelector('#url-name').value;
    let serviceName = document.querySelector('#service-name').value;
    fetch('/service', {
    method: 'post',
    headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json'
    },
    body: JSON.stringify({url:urlName, serviceName})
    }).then( res => {
      listContainer.appendChild(createListItem(urlName, "UNKNOWN"))
      getServices()
    });
}

const createListItem = (url, status) => {
  let li = document.createElement("li")
  li.id = url
  li.className = "card"

  let statusIndicator = createStatusIndicator(status)
  let title = createServiceTitle(url)
  let button = createDeleteButton(url)
  let wrapper = document.createElement("div")
  wrapper.className = "flex-wrapper"
  let flexItemWrapper = document.createElement("div")
  flexItemWrapper.className = "inner-flex-wrapper"
  flexItemWrapper.appendChild(statusIndicator)
  flexItemWrapper.appendChild(title)
  wrapper.appendChild(flexItemWrapper)
  wrapper.appendChild(button)
  li.appendChild(wrapper)

  return li
}

const createDeleteButton = (url) => {
  let btn = document.createElement("button")
  btn.innerHTML = "DELETE"
  btn.addEventListener("click", () => { removeService(url) })
  return btn
}

const createStatusIndicator = (status) => {
  let span = document.createElement("span")
  if(status === "OK") {
    span.className = "ok"
  } else if (status === "UNKNOWN") {
    span.className = "unknown"
  } else {
    span.className = "fail"
  }

  return span
}

const createServiceTitle = (url) => {
  let div = document.createElement("div")
  let title = document.createElement("h3")
  title.appendChild(document.createTextNode(url))
  div.appendChild(title)
  div.className = "title"
  return div
}

const getServices = () => {
  fetch(servicesRequest)
    .then(function(response) { return response.json(); })
    .then(function(serviceList) {
      listContainer.innerHTML = '';
      serviceList.forEach((service) => {
        let li = createListItem(service.url, service.status)
        listContainer.appendChild(li);
      });
    });
}

const removeService = (url) => {
  fetch('/service', {
    method: 'delete',
    headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json'
    },
    body: JSON.stringify({url})
    //}).then(res => {document.getElementById(url).remove()});
    }).then(res => { getServices() });
}


function poll() {
    console.log("polling")
    getServices();
    setTimeout(poll, 5000);
}

// initial call, or just call refresh directly
console.log("first poll")
poll()