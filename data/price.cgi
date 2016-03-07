#!/usr/bin/perl -w
$company = $ENV{'QUERY_STRING'};
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

for (my $i=100; $i >= 0; $i--) {
 print "<p>$i bottles of beer on the wall</p>";
}

print "<p> That's all folks! <\p>";
print "</html>"; 
