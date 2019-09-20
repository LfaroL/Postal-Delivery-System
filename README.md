# Postal-Delivery-System

Object-Oriented Programming in Java <br />
Feb - Mar 2016 <br />

<br />

<b>Postal Delivery System:</b> <br />
The application sets up offices then executes the commands that simulate the work of operations in postal offices. <br />
Results are written on a text file for each office. <br />

<br />

<b>How it works:</b> <br />
1. Initializes offices read from "Offices.txt" <br />
2. Initializes black-listed customers read from "Wanted.txt" <br />
3. Execute commands read from "Commands.txt" <br />

<br />

<b>Text Files:</b>  <br />
1. Offices.txt  <br />
&nbsp;&nbsp;&nbsp;&nbsp;- Contains a list of offices to be created in the beginning of the program <br />
&nbsp;&nbsp;&nbsp;&nbsp;- Offices have properties such as name, days to travel, storage space, and max package length. <br />
&nbsp;&nbsp;&nbsp;&nbsp;- Offices can accept or reject deliverables based on exceeding max length or lacking storage space. <br />

<br />
2. Wanted.txt  <br />
&nbsp;&nbsp;&nbsp;&nbsp;- Contains a list of criminals that are prevented from picking up packages <br />
&nbsp;&nbsp;&nbsp;&nbsp;- Wanted criminals have a name property and are separated by a new line. <br />
<br />
3. Commands.txt  <br />
&nbsp;&nbsp;&nbsp;&nbsp;- Contains a list of commands to be executed <br />
&nbsp;&nbsp;&nbsp;&nbsp;- List of Commands: <br />
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Day: Ends the current working day. Deliverables are sent out from each office if any.<br />
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Letter: Places a letter deliverable in the office<br />
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Package: Places a package deliverable in the office<br />
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Pickup: Pickup all deliverables designated to a recepient<br />
<br />


<b>Classes:</b> <br />
1. Main <br />
2. Office <br />
3. Network <br />
4. Deliverable <br />
4.1 Package <br />
4.2 Letter <br />
5. Logging <br />
<br />

<b>Basic UML Diagram:</b> <br />
<img src="PostalDeliveryUML.jpg" alt="Basic UML Diagram" width=482 height=537>

<br />
<b>Detailed UML Diagram:</b> <br />
<img src="PostalDeliveryUML-Detailed.png" alt="Detailed UML Diagram" width=482 height=537>

