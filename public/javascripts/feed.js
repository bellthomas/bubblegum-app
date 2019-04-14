function reload() {
    if(gettingPosts) {
        var refreshTime = Date.now();
        var newHead = parseInt(refreshTime / epochDuration);
        var headAtLastReload = parseInt(timeAtLastReload / epochDuration)

        if(newHead == headAtLastReload) {
            getEpoch(newHead, timeAtLastReload);
            timeAtLastReload = refreshTime;
        } else if (newHead == (headAtLastReload + 1)) {
            getEpoch(newHead);
            getEpoch(headAtLastReload, timeAtLastReload);
            timeAtLastReload = refreshTime;
        } else {
            // Clear board and reload fresh
            location.reload();
        }
    }
}

function setSmallestEpochShowing(newSmallest) {
    smallestEpochShowing = newSmallest;
    var date = new Date(newSmallest * epochDuration);
    var dateString = date.toLocaleTimeString();// +', '+ date.toLocaleDateString();
    if(parseInt(date.getTime() / 86400000) != parseInt(Date.now() / 86400000)) {
        dateString += ', '+ date.toLocaleDateString();
    }
    document.getElementById("loaded-back-to-label").innerText = "Loaded back to " + dateString;
}

function showAnOlderEpoch() {
    document.getElementById("load-more").classList.add("loading");
    var currentNumber = posts.size
    var currentEpoch = smallestEpochShowing - 1;

    // Step back to find the next oldest posts, to a maximum of 3 hours.
    while(currentEpoch >= smallestEpochShowing - 36 && posts.size == currentNumber) {
        getEpoch(currentEpoch);
        currentEpoch--;
    }

    setTimeout(function () {
        setSmallestEpochShowing(currentEpoch + 1);
        document.getElementById("load-more").classList.remove("loading");
    }, 500);
}
function getEpoch(num, from=0) {
    if(gettingPosts) {
        var xhr = new XMLHttpRequest();
        xhr.open('POST', getEpochPath);
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.onload = function() {
            if (xhr.status === 200) {
                consecutiveFails = 0;
                if(xhr.responseText.length > 0) {
                    try {
                        epochsLoaded.add(num);
                        var result = JSON.parse(xhr.responseText);
                        if('data' in result) {
                            result["data"].forEach(function (p) {
                                insertPost(p);
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
        };
        // console.log({ "epoch": num, "fromTime": from });
        xhr.send(JSON.stringify({ "epoch": num, "fromTime": from }));
    }
}

function insertPost(p) {
    if(posts.size == 0) {
        document.getElementById("posts-empty").hidden = true;
        document.getElementById("posts-loading").hidden = true;
    }
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
        if ('time' in k) {
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
            document.getElementById("posts").insertBefore(newDOMNode, closestDOMNode);
        } else {
            document.getElementById("posts").append(newDOMNode);
        }
    } else {
        document.getElementById("posts").append(newDOMNode);
    }

    if (getEmHeight("content-" + p["id"]) > 11) {
        document.getElementById("content-" + p["id"]).classList.add("more-content-fade");
        var showMore = document.createElement("button");
        showMore.setAttribute("id", "more-btn");
        showMore.classList.add("btn", "btn-link");
        showMore.textContent = "More...";
        showMore.style.padding = 0;
        showMore.onclick = function () {
            document.getElementById("content-" + p["id"]).classList.remove("hide", "more-content-fade");
            this.remove();
        };
        var referenceNode = document.getElementById("content-" + p["id"]);
        referenceNode.parentNode.insertBefore(showMore, referenceNode.nextSibling);
    } else {
        document.getElementById("content-" + p["id"]).classList.remove("hide");
    }
}

function simpleSanitise(str) {
    return str.replace("<", "&lt;").replace(">", "&gt;");
}

function postToHTML(p) {
    var entity = document.createElement("div");
    entity.setAttribute("class", "timeline-item");
    entity.dataset.time = p["time"];
    entity.dataset.owner = simpleSanitise(p["owner"]);
    entity.setAttribute("id", simpleSanitise(p["ownerHash"] + ":" + p["id"]));

    var date = new Date(p["time"]);
    var dateString = date.toLocaleTimeString();// +', '+ date.toLocaleDateString();
    if(parseInt(date.getTime() / 86400000) != parseInt(Date.now() / 86400000)) {
        dateString += ', '+ date.toLocaleDateString();
    }

    var ownerHash = '<code class="float-right hash-id">User #' + p["ownerHash"].substring(0,8) + '</code>';
    if(selfID == p["ownerHash"].valueOf()) {
        ownerHash = '<code class="float-right hash-id me">You</code>';
    }

    var threadURI = btoa(p["ownerHash"] + ":" + p["id"]);
    var markdown = LZString.decompressFromBase64(p["content"]);
    var content = md.render(markdown);

    var inner = '<div class="timeline-left">' +
        '<a class="timeline-icon"></a>' +
        '</div>';
    inner += '<div class="timeline-content">' +
        '<div class="post-tile tile">' +
        '<div class="tile-content">' +
        '<h5 class="post-headline">'+ simpleSanitise(p["owner"]) +'</h5>' +
        '<kbd class="date-badge">'+ dateString +'</kbd>';

    if('response' in p && p["response"].length > 0) {
        inner += '<kbd class="reply-badge">COMMENT</kbd>' +
            '<a class="no-decoration" href="'+threadLink.replace('$',btoa(p['response']))+'"><samp>Original Post →</samp></a>';
    }

    inner +=            '<div id="content-'+simpleSanitise(p["id"])+'" class="post-content hide"><div class="inner">'+ content +'</div></div>' +
        '</div>' +
        '<div class="tile-action">' +
        '<a class="float-right" href="'+threadLink.replace('$',threadURI)+'"><button class="btn btn-sm btn-primary">Thread</button></a>' +
        '<a class="float-right" style="margin-right:8px;" href="#" onclick="showReplyForm(\''+ simpleSanitise(p["ownerHash"] + ":" + p["id"]) +'\', \'' + p["owner"] + '\')"><button class="btn btn-sm">Reply</button></a><br>' +
        ownerHash +
        '</div>' +
        '</div>' +
        '</div>';

    entity.innerHTML = inner;

    // Fix bb://{}/{} links
    entity.querySelectorAll("[href]").forEach(function(e) {
        if(e.hasAttribute("href")) {
            e.target = "_blank";
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

    return entity;
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
    document.getElementById("newpost-modal-title").innerText = "New Reponse";
    document.getElementById("newpost-byline").innerHTML = "<em>Replying to '"+ownerName+"'</em>";
    document.getElementById("newpost-byline").style.display = "block";
    document.getElementById("newpost-hidden-response").value = response;
}

function resetNewPostForm() {
    document.getElementById("newpost-modal-title").innerText = "New Post";
    document.getElementById("newpost-byline").innerHTML = "";
    document.getElementById("newpost-byline").style.display = "none";
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