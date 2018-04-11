<!--
function open_window(path, win_width, win_height) {

var win_width = win_width;
var win_height = win_height;
var parameters = "left=0,top=0,toolbar=no,menubar=no,scrollbars=no,resizable=no,width=" + win_width + ",height=" + win_height;
var newWindow = window.open(path,'name',parameters);
newWindow.creator = self;
newWindow.focus();

return true;

}//open_window

function redirect(select) {
var page = select.options[select.selectedIndex].value
if (page!="#") document.location.href = page
}

self.name = "oldwindow";
function checkInput(form)
{
var b = navigator.appName
var TheURL="/software/main/search/message.html";
var selected;
selected = form.realm.value;
if ((form.q.value == "") && (selected == "software"))
{
if (b=="Netscape")
{
zview=window.open(TheURL,"view","toolbar=no,menubar=no,scrollbars=no,resizable=yes,width=315,height=348");
}
else
{
zview=window.open(TheURL,"view","toolbar=no,menubar=no,scrollbars=no,resizable=yes,width=315,height=370");
}
zview.creator=self;
}
else
{
form.submit();
}
return false;
}
//-->