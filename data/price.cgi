#!/usr/bin/perl -w
$company = $ENV{'QUERY_STRING'};
$method = $ENV{'REQUEST_METHOD'};
$server = $ENV{'SERVER_NAME'};
$remote_addr = $ENV{'REMOTE_ADDR'};
$remote_host = $ENV{'REMOTE_HOST'};
$server_port = $ENV{'SERVER_PORT'};
$server_protocol = $ENV{'SERVER_PROTOCOL'};

print "Content-Type: text/html\r\n";
print "\r\n";
print "<html>";
print "<h1>Hello! The price is ";

if ($company =~ /appl/) {
 my $var_rand = rand();
 print 450 + 10 * $var_rand;
} else {
 print "150";
}
print "</h1>";

print "<p> Req_method : $method, Server_name: $server</p>";
print "<p> Remote_addr: $remote_addr, Remote_host: $remote_addr</p>";
print "<p>  Server_port: $server_port, Server_protocol: $server_protocol</p>";


for (my $i=100; $i >= 0; $i--) {
 print "<p>$i bottles of beer on the wall</p>";
}

print "<p> That's all folks! <\p>";
print "</html>"; 
