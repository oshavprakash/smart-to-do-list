
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Smart To-Do List</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background: #f2f0ff;
            color: #292445;
            max-width: 700px;
            margin: 50px auto;
            padding: 20px;
        }

        h1 {
            color: #6c4bd8;
        }

        .card {
            background: white;
            padding: 24px;
            border-radius: 14px;
            box-shadow: 0 4px 15px #00000010;
        }

        input, textarea, select, button {
            box-sizing: border-box;
            width: 100%;
            margin-top: 12px;
            padding: 12px;
            border: 1px solid #ddd6f5;
            border-radius: 8px;
            font-size: 15px;
        }

        button {
            background: #7655df;
            color: white;
            border: none;
            cursor: pointer;
        }
    </style>
</head>
<body>
    <h1>Smart To-Do List</h1>

    <div class="card">
        <h2>Add a task</h2>

        <form id="taskForm">
            <input type="text" id="title" placeholder="Task title" required>

            <textarea id="description" placeholder="Task description"></textarea>

            <select id="priority">
                <option value="Low">Low priority</option>
                <option value="Medium" selected>Medium priority</option>
                <option value="High">High priority</option>
            </select>

            <input type="date" id="dueDate">

            <button type="submit">Add Task</button>
        </form>

        <p id="message">Your tasks will appear here soon!</p>
    </div>

    <script>
        document.getElementById("taskForm").addEventListener("submit", function(event) {
            event.preventDefault();
            document.getElementById("message").textContent =
                "Your first task form is working! We'll connect it to Java next.";
        });
    </script>
</body>
</html>
