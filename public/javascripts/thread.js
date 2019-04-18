function checkForMoreButton(id) {
    if(getEmHeight(id) > 11) {
        document.getElementById(id).classList.add("more-content-fade");
        var showMore = document.createElement("button");
        showMore.setAttribute("id", "more-btn");
        showMore.classList.add("btn", "btn-link");
        showMore.textContent = "More...";
        showMore.style.padding = 0;
        showMore.onclick = function() {
            document.getElementById(id).classList.remove("hide");
            this.remove();
        };
        var referenceNode = document.getElementById(id);
        referenceNode.parentNode.insertBefore(showMore, referenceNode.nextSibling);
    } else {
        document.getElementById(id).classList.remove("hide");
    }
}

function getEmHeight(id) {
    return document.getElementById(id).clientHeight / parseFloat(
        getComputedStyle(document.querySelector('body'))['font-size']
    )
}

function showReplyForm(response, ownerName) {
    setupNewPostFormForResponse(response, ownerName);
    document.getElementById("modal-newpost").classList.add("active");
}

function setupNewPostFormForResponse(response, ownerName) {
    document.getElementById("newpost-byline").innerHTML = "<em>Replying to '"+ownerName+"'</em>";
    document.getElementById("newpost-hidden-response").value = response;
}

function resetNewPostForm() {
    document.getElementById("newpost-byline").innerHTML = "";
    document.getElementById("newpost-hidden-response").value = "";
    document.getElementById("newpost-textarea").value = "";
}

function prepareForNewPostSubmit() {
    var content = mde.value();
    if(content.length > 0) {
        content = content.replace(/bb:\/\/self\//gi, selfURL.replace("$", ""));
        var compressed = LZString.compressToBase64(content);
        document.getElementById("newpost-content").value = compressed;
        return true;
    }
    return false;
}

function getComments(id, showActivity=true) {
    var loader = document.getElementById("responses-loading-"+id);
    loader.style.display = "block";
    if(showActivity) loader.classList.add("loading");
    loader.innerHTML = "Recheck &nbsp;<i class='icon icon-refresh' style='font-size: 0.4em;vertical-align: inherit;'></i>";
    loader.style.margin = "-8px 0";

    var xhr = new XMLHttpRequest();
    xhr.open('POST', getCommentsPath);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onload = function() {
        if (xhr.status === 200) {
            consecutiveFails = 0;
            if(xhr.responseText.length > 0) {
                try {
                    var result = JSON.parse(xhr.responseText);
                    if('data' in result) {
                        result["data"].forEach(function (p) {
                            insertComment(id, p);
                        });
                    }
                } catch(e) {
                    console.log(e);
                }
            }
        }
        else if (xhr.status !== 200) {
            consecutiveFails++;
            console.log('Request failed.  Returned status of ' + xhr.status + ": " + xhr.responseText);
        }

        window.setTimeout(function() {
            loader.classList.remove("loading");
        }, 500);
    };

    xhr.send(JSON.stringify({ "pid": id }));
}

function insertComment(parent, p) {
    var closestBefore = Number.MAX_SAFE_INTEGER;
    var closestID = "";
    var temp;
    var unique = true;
    posts.forEach(function (k) {
        if ('id' in k) {
            if(k["id"] == p["id"]) {
                unique = false;
                return;
            }
        }
        if ('time' in k && 'response' in k && k['response'] == p['response']) {
            temp = p["time"] - k["time"];
            if (temp > 0 && temp < closestBefore) {
                closestBefore = temp;
                closestID = k["ownerHash"] + ":" + k["id"];
            }
        }
    });


    if(!unique) return;
    posts.add(p);
    var newDOMNode = postToHTML(p);

    if (closestID.length > 0) {
        var closestDOMNode = document.getElementById(closestID);
        if (closestDOMNode) {
            document.getElementById("responses-"+parent).insertBefore(newDOMNode, closestDOMNode.parentElement);
        } else {
            document.getElementById("responses-"+parent).append(newDOMNode);
        }
    } else {
        document.getElementById("responses-"+parent).append(newDOMNode);
    }

    var contentNode = document.getElementById("content-" + p["ownerHash"] + ":" + p["id"]);
    if (getEmHeight("content-" + p["ownerHash"] + ":" + p["id"]) > 11) {
        contentNode.classList.add("more-content-fade");
        var showMore = document.createElement("button");
        showMore.setAttribute("id", "more-btn");
        showMore.classList.add("btn", "btn-link");
        showMore.textContent = "More...";
        showMore.style.padding = 0;
        showMore.onclick = function () {
            contentNode.classList.remove("hide", "more-content-fade");
            this.remove();
        };
        contentNode.parentNode.insertBefore(showMore, contentNode.nextSibling);
    } else {
        contentNode.classList.remove("hide");
    }
}

function simpleSanitise(str) {
    return str.replace("<", "&lt;").replace(">", "&gt;");
}

function postToHTML(p) {
    var compoundPostID = simpleSanitise(p["ownerHash"] + ':' + p["id"]);
    var entity = document.createElement("div");
    entity.setAttribute("class", "timeline-item");
    entity.dataset.time = p["time"];
    entity.dataset.owner = simpleSanitise(p["owner"]);

    var date = new Date(p["time"]);
    var dateString = date.toLocaleTimeString();// +', '+ date.toLocaleDateString();
    if(parseInt(date.getTime() / 86400000) != parseInt(Date.now() / 86400000)) {
        dateString += ', '+ date.toLocaleDateString();
    }

    var ownerHash = '<code class="float-right hash-id">User #' + p["ownerHash"].substring(0,8) + '</code>';
    if(selfID == p["ownerHash"].valueOf()) {
        ownerHash = '<code class="float-right hash-id me">You</code>';
    }

    var threadURI = btoa(compoundPostID);
    var content = md.render(LZString.decompressFromBase64(p["content"]));
    var depth = parseInt(document.getElementById(simpleSanitise(p["response"])).dataset.depth) + 1;

    var inner = '<div class="timeline-left">' +
        '<a class="timeline-icon"></a>' +
        '</div>';
    inner += '<div id="'+compoundPostID+'" class="timeline-content" data-depth="'+depth+'">' +
        '<div class="post-tile tile">' +
        '<div class="tile-content">' +
        '<h5 class="post-headline">'+ simpleSanitise(p["owner"]) +'</h5>' +
        '<kbd class="date-badge">'+ dateString +'</kbd>';


    inner += '<div id="content-'+ compoundPostID +'" class="post-content hide"><div class="inner">'+ content +'</div></div>' +
        '<button id="show-comments-'+ compoundPostID +'" onclick="startShowingComments(\''+ compoundPostID +'\', '+depth+')" class="btn btn-link" style="padding: 0;font-size: 0.8em;font-weight: 500;">Show Comments &nbsp;<i class="icon icon-arrow-right"></i></button>' +
        '</div>' +
        '<div class="tile-action">' +
        '<a class="float-right" href="'+threadLink.replace('$',threadURI)+'"><button class="btn btn-sm btn-primary">Thread</button></a>' +
        '<a class="float-right" style="margin-right:8px;" href="#" onclick="showReplyForm(\''+ compoundPostID +'\', \'' + p["owner"] + '\')"><button class="btn btn-sm">Reply</button></a><br>' +
        ownerHash +
        '</div>' +
        '</div>' +
        '</div>';

    entity.innerHTML = inner;
    fixBBLinks(entity);
    return entity;
}

function fixBBLinks(entity) {
    // Fix bb://{}/{} links
    entity.querySelectorAll("[href]").forEach(function(e) {
        if(e.hasAttribute("href")) {
            if(e.href.includes("bb://")) {
                e.href = e.href.replace("bb://", resourceURL.replace("$/$", ""));
            }
        }
    });

    entity.querySelectorAll("[src]").forEach(function(e) {
        if(e.hasAttribute("src")) {
            if(e.src.includes("bb://")) {
                e.src = e.src.replace("bb://", resourceURL.replace("$/$", ""));
            }
        }
    });
}

function reload(showActivity) {
    shownResponseSets.forEach(function(pid) {
        getComments(pid, showActivity);
    });
}

function startShowingComments(pid, depth) {
    var w = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
    if ((w <= 600 && depth >= 2) || depth >= 3) {
        window.location.href = threadLink.replace('$', btoa(pid));
    }
    else {
        var commentsSection = document.createElement("div");
        var inner = '<div class="divider text-center" data-content="Comments"></div>' +
            '<button id="responses-loading-' + pid + '" onclick="getComments(\'' + pid + '\')" class="btn btn-link loading load-comments"></button>' +
            '<div id="responses-' + pid + '" class="timeline"></div>';
        commentsSection.innerHTML = inner;

        var reference = document.getElementById("show-comments-" + pid);
        reference.parentNode.parentNode.parentNode.insertBefore(commentsSection, reference.parentNode.parentNode.nextSibling);
        reference.remove();

        shownResponseSets.push(pid);
        getComments(pid);
    }
}

// <div class="divider text-center" data-content="Comments"></div>
//
// <button id="responses-loading-@{post.getOwner}:@{post.getID}" onclick="getComments('@{post.getOwner}:@{post.getID}')" class="btn btn-link loading load-comments"></button>
// <div id="responses-@{post.getOwner}:@{post.getID}" class="timeline" data-depth="1">
//
// </div>