/*
 * file:       ProjectFile.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2002-2006
 * date:       15/08/2002
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.mpxj.common.NumberHelper;
import net.sf.mpxj.listener.ProjectListener;

/**
 * This class represents a project plan.
 */
public final class ProjectFile implements TaskContainer
{
   /**
    * This method is provided to allow child tasks that have been created
    * programmatically to be added as a record to the main file.
    *
    * @param task task created as a child of another task
    */
   void addTask(Task task)
   {
      m_allTasks.add(task);
   }

   /**
    * This method allows a task to be added to the file programatically.
    *
    * @return new task object
    */
   public Task addTask()
   {
      Task task = new Task(this, (Task) null);
      m_allTasks.add(task);
      m_childTasks.add(task);
      return (task);
   }

   /**
    * This method is used to remove a task from the project.
    *
    * @param task task to be removed
    */
   public void removeTask(Task task)
   {
      //
      // Remove the task from the file and its parent task
      //
      m_allTasks.remove(task);
      m_taskUniqueIDMap.remove(task.getUniqueID());
      m_taskIDMap.remove(task.getID());

      Task parentTask = task.getParentTask();
      if (parentTask != null)
      {
         parentTask.removeChildTask(task);
      }
      else
      {
         m_childTasks.remove(task);
      }

      //
      // Remove all resource assignments
      //
      Iterator<ResourceAssignment> iter = m_allResourceAssignments.iterator();
      while (iter.hasNext() == true)
      {
         ResourceAssignment assignment = iter.next();
         if (assignment.getTask() == task)
         {
            Resource resource = assignment.getResource();
            if (resource != null)
            {
               resource.removeResourceAssignment(assignment);
            }
            iter.remove();
         }
      }

      //
      // Recursively remove any child tasks
      //
      while (true)
      {
         List<Task> childTaskList = task.getChildTasks();
         if (childTaskList.isEmpty() == true)
         {
            break;
         }

         removeTask(childTaskList.get(0));
      }
   }

   /**
    * This method can be called to ensure that the IDs of all
    * tasks in this project are sequential, and start from an
    * appropriate point. If tasks are added to and removed from
    * the list of tasks, then the project is loaded into Microsoft
    * project, if the ID values have gaps in the sequence, there will
    * be blank task rows shown.
    */
   public void renumberTaskIDs()
   {
      if (m_allTasks.isEmpty() == false)
      {
         Collections.sort(m_allTasks);
         Task firstTask = m_allTasks.get(0);
         int id = NumberHelper.getInt(firstTask.getID());
         if (id != 0)
         {
            id = 1;
         }

         for (Task task : m_allTasks)
         {
            task.setID(Integer.valueOf(id++));
         }
      }
   }

   /**
    * This method can be called to ensure that the IDs of all
    * resources in this project are sequential, and start from an
    * appropriate point. If resources are added to and removed from
    * the list of resources, then the project is loaded into Microsoft
    * project, if the ID values have gaps in the sequence, there will
    * be blank resource rows shown.
    */
   public void renumberResourceIDs()
   {
      if (m_allResources.isEmpty() == false)
      {
         Collections.sort(m_allResources);
         int id = 1;

         for (Resource resource : m_allResources)
         {
            resource.setID(Integer.valueOf(id++));
         }
      }
   }

   /**
    * Renumbers all task unique IDs.
    */
   private void renumberTaskUniqueIDs()
   {
      Task firstTask = getTaskByID(Integer.valueOf(0));
      int uid = (firstTask == null ? 1 : 0);

      for (Task task : m_allTasks)
      {
         task.setUniqueID(Integer.valueOf(uid++));
      }
   }

   /**
    * Renumbers all resource unique IDs.
    */
   private void renumberResourceUniqueIDs()
   {
      int uid = 1;

      for (Resource resource : m_allResources)
      {
         resource.setUniqueID(Integer.valueOf(uid++));
      }
   }

   /**
    * Renumbers all assignment unique IDs.
    */
   private void renumberAssignmentUniqueIDs()
   {
      int uid = 1;

      for (ResourceAssignment assignment : m_allResourceAssignments)
      {
         assignment.setUniqueID(Integer.valueOf(uid++));
      }
   }

   /**
    * Renumbers all calendar unique IDs.
    */
   private void renumberCalendarUniqueIDs()
   {
      int uid = 1;

      for (ProjectCalendar calendar : m_calendars)
      {
         calendar.setUniqueID(Integer.valueOf(uid++));
      }
   }

   /**
    * This method is called to ensure that all unique ID values
    * held by MPXJ are within the range supported by MS Project.
    * If any of these values fall outside of this range, the unique IDs
    * of the relevant entities are renumbered.
    */
   public void validateUniqueIDsForMicrosoftProject()
   {
      if (!m_allTasks.isEmpty())
      {
         for (Task task : m_allTasks)
         {
            if (NumberHelper.getInt(task.getUniqueID()) > MS_PROJECT_MAX_UNIQUE_ID)
            {
               renumberTaskUniqueIDs();
               break;
            }
         }
      }

      if (!m_allResources.isEmpty())
      {
         for (Resource resource : m_allResources)
         {
            if (NumberHelper.getInt(resource.getUniqueID()) > MS_PROJECT_MAX_UNIQUE_ID)
            {
               renumberResourceUniqueIDs();
               break;
            }
         }
      }

      if (!m_allResourceAssignments.isEmpty())
      {
         for (ResourceAssignment assignment : m_allResourceAssignments)
         {
            if (NumberHelper.getInt(assignment.getUniqueID()) > MS_PROJECT_MAX_UNIQUE_ID)
            {
               renumberAssignmentUniqueIDs();
               break;
            }
         }
      }

      if (!m_calendars.isEmpty())
      {
         for (ProjectCalendar calendar : m_calendars)
         {
            if (NumberHelper.getInt(calendar.getUniqueID()) > MS_PROJECT_MAX_UNIQUE_ID)
            {
               renumberCalendarUniqueIDs();
               break;
            }
         }
      }
   }

   /**
    * Microsoft Project bases the order of tasks displayed on their ID
    * value. This method takes the hierarchical structure of tasks
    * represented in MPXJ and renumbers the ID values to ensure that
    * this structure is displayed as expected in Microsoft Project. This
    * is typically used to deal with the case where a hierarchical task
    * structure has been created programatically in MPXJ.  
    */
   public void synchronizeTaskIDToHierarchy()
   {
      m_allTasks.clear();

      int currentID = (getTaskByID(Integer.valueOf(0)) == null ? 1 : 0);
      for (Task task : m_childTasks)
      {
         task.setID(Integer.valueOf(currentID++));
         m_allTasks.add(task);
         currentID = synchroizeTaskIDToHierarchy(task, currentID);
      }
   }

   /**
    * Called recursively to renumber child task IDs.
    * 
    * @param parentTask parent task instance
    * @param currentID current task ID
    * @return updated current task ID
    */
   private int synchroizeTaskIDToHierarchy(Task parentTask, int currentID)
   {
      for (Task task : parentTask.getChildTasks())
      {
         task.setID(Integer.valueOf(currentID++));
         m_allTasks.add(task);
         currentID = synchroizeTaskIDToHierarchy(task, currentID);
      }
      return currentID;
   }

   /**
    * This method is used to retrieve a list of all of the top level tasks
    * that are defined in this project file.
    *
    * @return list of tasks
    */
   @Override public List<Task> getChildTasks()
   {
      return (m_childTasks);
   }

   /**
    * This method is used to retrieve a list of all of the tasks
    * that are defined in this project file.
    *
    * @return list of all tasks
    */
   public List<Task> getAllTasks()
   {
      return (m_allTasks);
   }

   /**
    * Used to set whether WBS numbers are automatically created.
    *
    * @param flag true if automatic WBS required.
    */
   public void setAutoWBS(boolean flag)
   {
      m_autoWBS = flag;
   }

   /**
    * Used to set whether outline level numbers are automatically created.
    *
    * @param flag true if automatic outline level required.
    */
   public void setAutoOutlineLevel(boolean flag)
   {
      m_autoOutlineLevel = flag;
   }

   /**
    * Used to set whether outline numbers are automatically created.
    *
    * @param flag true if automatic outline number required.
    */
   public void setAutoOutlineNumber(boolean flag)
   {
      m_autoOutlineNumber = flag;
   }

   /**
    * Used to set whether the task unique ID field is automatically populated.
    *
    * @param flag true if automatic unique ID required.
    */
   public void setAutoTaskUniqueID(boolean flag)
   {
      m_autoTaskUniqueID = flag;
   }

   /**
    * Used to set whether the calendar unique ID field is automatically populated.
    *
    * @param flag true if automatic unique ID required.
    */
   public void setAutoCalendarUniqueID(boolean flag)
   {
      m_autoCalendarUniqueID = flag;
   }

   /**
    * Used to set whether the assignment unique ID field is automatically populated.
    *
    * @param flag true if automatic unique ID required.
    */
   public void setAutoAssignmentUniqueID(boolean flag)
   {
      m_autoAssignmentUniqueID = flag;
   }

   /**
    * Used to set whether the task ID field is automatically populated.
    *
    * @param flag true if automatic ID required.
    */
   public void setAutoTaskID(boolean flag)
   {
      m_autoTaskID = flag;
   }

   /**
    * Retrieve the flag that determines whether WBS is generated
    * automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoWBS()
   {
      return (m_autoWBS);
   }

   /**
    * Retrieve the flag that determines whether outline level is generated
    * automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoOutlineLevel()
   {
      return (m_autoOutlineLevel);
   }

   /**
    * Retrieve the flag that determines whether outline numbers are generated
    * automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoOutlineNumber()
   {
      return (m_autoOutlineNumber);
   }

   /**
    * Retrieve the flag that determines whether the task unique ID
    * is generated automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoTaskUniqueID()
   {
      return (m_autoTaskUniqueID);
   }

   /**
    * Retrieve the flag that determines whether the calendar unique ID
    * is generated automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoCalendarUniqueID()
   {
      return (m_autoCalendarUniqueID);
   }

   /**
    * Retrieve the flag that determines whether the assignment unique ID
    * is generated automatically.
    *
    * @return boolean, default is true.
    */
   public boolean getAutoAssignmentUniqueID()
   {
      return (m_autoAssignmentUniqueID);
   }

   /**
    * Retrieve the flag that determines whether the task ID
    * is generated automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoTaskID()
   {
      return (m_autoTaskID);
   }

   /**
    * This method is used to retrieve the next unique ID for a task.
    *
    * @return next unique ID
    */
   public int getNextTaskUniqueID()
   {
      return (++m_taskUniqueID);
   }

   /**
    * This method is used to retrieve the next unique ID for a calendar.
    *
    * @return next unique ID
    */
   public int getNextCalendarUniqueID()
   {
      return (++m_calendarUniqueID);
   }

   /**
    * This method is used to retrieve the next unique ID for an assignment.
    *
    * @return next unique ID
    */
   int getNextAssignmentUniqueID()
   {
      return (++m_assignmentUniqueID);
   }

   /**
    * This method is used to retrieve the next ID for a task.
    *
    * @return next ID
    */
   public int getNextTaskID()
   {
      return (++m_taskID);
   }

   /**
    * Used to set whether the resource unique ID field is automatically populated.
    *
    * @param flag true if automatic unique ID required.
    */
   public void setAutoResourceUniqueID(boolean flag)
   {
      m_autoResourceUniqueID = flag;
   }

   /**
    * Used to set whether the resource ID field is automatically populated.
    *
    * @param flag true if automatic ID required.
    */
   public void setAutoResourceID(boolean flag)
   {
      m_autoResourceID = flag;
   }

   /**
    * Retrieve the flag that determines whether the resource unique ID
    * is generated automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoResourceUniqueID()
   {
      return (m_autoResourceUniqueID);
   }

   /**
    * Retrieve the flag that determines whether the resource ID
    * is generated automatically.
    *
    * @return boolean, default is false.
    */
   public boolean getAutoResourceID()
   {
      return (m_autoResourceID);
   }

   /**
    * This method is used to retrieve the next unique ID for a resource.
    *
    * @return next unique ID
    */
   public int getNextResourceUniqueID()
   {
      return (++m_resourceUniqueID);
   }

   /**
    * This method is used to retrieve the next ID for a resource.
    *
    * @return next ID
    */
   public int getNextResourceID()
   {
      return (++m_resourceID);
   }

   /**
    * This method is used to add a new calendar to the file.
    *
    * @return new calendar object
    */
   public ProjectCalendar addCalendar()
   {
      ProjectCalendar calendar = new ProjectCalendar(this);
      m_calendars.add(calendar);
      return (calendar);
   }

   /**
    * Removes a calendar.
    *
    * @param calendar calendar to be removed
    */
   public void removeCalendar(ProjectCalendar calendar)
   {
      if (m_calendars.contains(calendar))
      {
         m_calendars.remove(calendar);
      }

      Resource resource = calendar.getResource();
      if (resource != null)
      {
         resource.setResourceCalendar(null);
      }

      calendar.setParent(null);
   }

   /**
    * This is a convenience method used to add a calendar called
    * "Standard" to the file, and populate it with a default working week
    * and default working hours.
    *
    * @return a new default calendar
    */
   public ProjectCalendar addDefaultBaseCalendar()
   {
      ProjectCalendar calendar = addCalendar();

      calendar.setName(ProjectCalendar.DEFAULT_BASE_CALENDAR_NAME);

      calendar.setWorkingDay(Day.SUNDAY, false);
      calendar.setWorkingDay(Day.MONDAY, true);
      calendar.setWorkingDay(Day.TUESDAY, true);
      calendar.setWorkingDay(Day.WEDNESDAY, true);
      calendar.setWorkingDay(Day.THURSDAY, true);
      calendar.setWorkingDay(Day.FRIDAY, true);
      calendar.setWorkingDay(Day.SATURDAY, false);

      calendar.addDefaultCalendarHours();

      return (calendar);
   }

   /**
    * This is a protected convenience method to add a default derived
    * calendar.
    *
    * @return new ProjectCalendar instance
    */
   public ProjectCalendar addDefaultDerivedCalendar()
   {
      ProjectCalendar calendar = addCalendar();

      calendar.setWorkingDay(Day.SUNDAY, DayType.DEFAULT);
      calendar.setWorkingDay(Day.MONDAY, DayType.DEFAULT);
      calendar.setWorkingDay(Day.TUESDAY, DayType.DEFAULT);
      calendar.setWorkingDay(Day.WEDNESDAY, DayType.DEFAULT);
      calendar.setWorkingDay(Day.THURSDAY, DayType.DEFAULT);
      calendar.setWorkingDay(Day.FRIDAY, DayType.DEFAULT);
      calendar.setWorkingDay(Day.SATURDAY, DayType.DEFAULT);

      return (calendar);
   }

   /**
    * This method retrieves the list of calendars defined in
    * this file.
    *
    * @return list of calendars
    */
   public List<ProjectCalendar> getCalendars()
   {
      return (m_calendars);
   }

   /**
    * This method is used to retrieve the project header record.
    *
    * @return project header object
    */
   public ProjectHeader getProjectHeader()
   {
      return (m_projectHeader);
   }

   /**
    * This method is used to add a new resource to the file.
    *
    * @return new resource object
    */
   public Resource addResource()
   {
      Resource resource = new Resource(this);
      m_allResources.add(resource);
      return (resource);
   }

   /**
    * This method is used to remove a resource from the project.
    *
    * @param resource resource to be removed
    */
   public void removeResource(Resource resource)
   {
      m_allResources.remove(resource);
      m_resourceUniqueIDMap.remove(resource.getUniqueID());
      m_resourceIDMap.remove(resource.getID());

      Iterator<ResourceAssignment> iter = m_allResourceAssignments.iterator();
      Integer resourceUniqueID = resource.getUniqueID();
      while (iter.hasNext() == true)
      {
         ResourceAssignment assignment = iter.next();
         if (NumberHelper.equals(assignment.getResourceUniqueID(), resourceUniqueID))
         {
            assignment.getTask().removeResourceAssignment(assignment);
            iter.remove();
         }
      }

      ProjectCalendar calendar = resource.getResourceCalendar();
      if (calendar != null)
      {
         calendar.remove();
      }
   }

   /**
    * Retrieves a list of all resources in this project.
    *
    * @return list of all resources
    */
   public List<Resource> getAllResources()
   {
      return (m_allResources);
   }

   /**
    * Retrieves a list of all resource assignments in this project.
    *
    * @return list of all resources
    */
   public List<ResourceAssignment> getAllResourceAssignments()
   {
      return (m_allResourceAssignments);
   }

   /**
    * This method is provided to allow resource assignments that have been
    * created programatically to be added as a record to the main file.
    *
    * @param assignment Resource assignment created as part of a task
    */
   void addResourceAssignment(ResourceAssignment assignment)
   {
      m_allResourceAssignments.add(assignment);
   }

   /**
    * This method removes a resource assignment from the internal storage
    * maintained by the project file.
    *
    * @param assignment resource assignment to remove
    */
   void removeResourceAssignment(ResourceAssignment assignment)
   {
      m_allResourceAssignments.remove(assignment);
      assignment.getTask().removeResourceAssignment(assignment);
      Resource resource = assignment.getResource();
      if (resource != null)
      {
         resource.removeResourceAssignment(assignment);
      }
   }

   /**
    * This method has been provided to allow the subclasses to
    * instantiate ResourecAssignment instances.
    *
    * @param task parent task
    * @return new resource assignment instance
    */
   public ResourceAssignment newResourceAssignment(Task task)
   {
      return (new ResourceAssignment(this, task));
   }

   /**
    * Retrieves the named calendar. This method will return
    * null if the named calendar is not located.
    *
    * @param calendarName name of the required calendar
    * @return ProjectCalendar instance
    */
   public ProjectCalendar getCalendarByName(String calendarName)
   {
      ProjectCalendar calendar = null;

      if (calendarName != null && calendarName.length() != 0)
      {
         Iterator<ProjectCalendar> iter = m_calendars.iterator();
         while (iter.hasNext() == true)
         {
            calendar = iter.next();
            String name = calendar.getName();

            if ((name != null) && (name.equalsIgnoreCase(calendarName) == true))
            {
               break;
            }

            calendar = null;
         }
      }

      return (calendar);
   }

   /**
    * Retrieves the calendar referred to by the supplied unique ID
    * value. This method will return null if the required calendar is not
    * located.
    *
    * @param calendarID calendar unique ID
    * @return ProjectCalendar instance
    */
   public ProjectCalendar getCalendarByUniqueID(Integer calendarID)
   {
      return (m_calendarUniqueIDMap.get(calendarID));
   }

   /**
    * This method is used to calculate the duration of work between two fixed
    * dates according to the work schedule defined in the named calendar. The
    * calendar used is the "Standard" calendar. If this calendar does not exist,
    * and exception will be thrown.
    *
    * @param startDate start of the period
    * @param endDate end of the period
    * @return new Duration object
    * @throws MPXJException normally when no Standard calendar is available
    */
   public Duration getDuration(Date startDate, Date endDate) throws MPXJException
   {
      return (getDuration("Standard", startDate, endDate));
   }

   /**
    * This method is used to calculate the duration of work between two fixed
    * dates according to the work schedule defined in the named calendar.
    * The name of the calendar to be used is passed as an argument.
    *
    * @param calendarName name of the calendar to use
    * @param startDate start of the period
    * @param endDate end of the period
    * @return new Duration object
    * @throws MPXJException normally when no Standard calendar is available
    */
   public Duration getDuration(String calendarName, Date startDate, Date endDate) throws MPXJException
   {
      ProjectCalendar calendar = getCalendarByName(calendarName);

      if (calendar == null)
      {
         throw new MPXJException(MPXJException.CALENDAR_ERROR + ": " + calendarName);
      }

      return (calendar.getDuration(startDate, endDate));
   }

   /**
    * This method allows an arbitrary task to be retrieved based
    * on its ID field.
    *
    * @param id task identified
    * @return the requested task, or null if not found
    */
   public Task getTaskByID(Integer id)
   {
      return (m_taskIDMap.get(id));
   }

   /**
    * This method allows an arbitrary task to be retrieved based
    * on its UniqueID field.
    *
    * @param id task identified
    * @return the requested task, or null if not found
    */
   public Task getTaskByUniqueID(Integer id)
   {
      return (m_taskUniqueIDMap.get(id));
   }

   /**
    * This method allows an arbitrary resource to be retrieved based
    * on its ID field.
    *
    * @param id resource identified
    * @return the requested resource, or null if not found
    */
   public Resource getResourceByID(Integer id)
   {
      return (m_resourceIDMap.get(id));
   }

   /**
    * This method allows an arbitrary resource to be retrieved based
    * on its UniqueID field.
    *
    * @param id resource identified
    * @return the requested resource, or null if not found
    */
   public Resource getResourceByUniqueID(Integer id)
   {
      return (m_resourceUniqueIDMap.get(id));
   }

   /**
    * This method is used to recreate the hierarchical structure of the
    * project file from scratch. The method sorts the list of all tasks,
    * then iterates through it creating the parent-child structure defined
    * by the outline level field.
    */
   public void updateStructure()
   {
      if (m_allTasks.size() > 1)
      {
         Collections.sort(m_allTasks);
         m_childTasks.clear();

         Task lastTask = null;
         int lastLevel = -1;

         for (Task task : m_allTasks)
         {
            task.clearChildTasks();
            Task parent = null;
            if (!task.getNull())
            {
               int level = NumberHelper.getInt(task.getOutlineLevel());

               if (lastTask != null)
               {
                  if (level == lastLevel || task.getNull())
                  {
                     parent = lastTask.getParentTask();
                     level = lastLevel;
                  }
                  else
                  {
                     if (level > lastLevel)
                     {
                        parent = lastTask;
                     }
                     else
                     {
                        while (level <= lastLevel)
                        {
                           parent = lastTask.getParentTask();

                           if (parent == null)
                           {
                              break;
                           }

                           lastLevel = NumberHelper.getInt(parent.getOutlineLevel());
                           lastTask = parent;
                        }
                     }
                  }
               }

               lastTask = task;
               lastLevel = level;

               if (getAutoWBS() || task.getWBS() == null)
               {
                  task.generateWBS(parent);
               }

               if (getAutoOutlineNumber())
               {
                  task.generateOutlineNumber(parent);
               }
            }

            if (parent == null)
            {
               m_childTasks.add(task);
            }
            else
            {
               parent.addChildTask(task);
            }
         }
      }
   }

   /**
    * This method is called to ensure that after a project file has been
    * read, the cached unique ID values used to generate new unique IDs
    * start after the end of the existing set of unique IDs.
    */
   public void updateUniqueCounters()
   {
      //
      // Update task unique IDs
      //
      for (Task task : m_allTasks)
      {
         int uniqueID = NumberHelper.getInt(task.getUniqueID());
         if (uniqueID > m_taskUniqueID)
         {
            m_taskUniqueID = uniqueID;
         }
      }

      //
      // Update resource unique IDs
      //
      for (Resource resource : m_allResources)
      {
         int uniqueID = NumberHelper.getInt(resource.getUniqueID());
         if (uniqueID > m_resourceUniqueID)
         {
            m_resourceUniqueID = uniqueID;
         }
      }

      //
      // Update calendar unique IDs
      //
      for (ProjectCalendar calendar : m_calendars)
      {
         int uniqueID = NumberHelper.getInt(calendar.getUniqueID());
         if (uniqueID > m_calendarUniqueID)
         {
            m_calendarUniqueID = uniqueID;
         }
      }

      //
      // Update assignment unique IDs
      //
      for (ResourceAssignment assignment : m_allResourceAssignments)
      {
         int uniqueID = NumberHelper.getInt(assignment.getUniqueID());
         if (uniqueID > m_assignmentUniqueID)
         {
            m_assignmentUniqueID = uniqueID;
         }
      }
   }

   /**
    * Find the earliest task start date. We treat this as the
    * start date for the project.
    *
    * @return start date
    */
   public Date getStartDate()
   {
      Date startDate = null;

      for (Task task : m_allTasks)
      {
         //
         // If a hidden "summary" task is present we ignore it
         //
         if (NumberHelper.getInt(task.getUniqueID()) == 0)
         {
            continue;
         }

         //
         // Select the actual or forecast start date. Note that the
         // behaviour is different for milestones. The milestone end date
         // is always correct, the milestone start date may be different
         // to reflect a missed deadline.
         //
         Date taskStartDate;
         if (task.getMilestone() == true)
         {
            taskStartDate = task.getActualFinish();
            if (taskStartDate == null)
            {
               taskStartDate = task.getFinish();
            }
         }
         else
         {
            taskStartDate = task.getActualStart();
            if (taskStartDate == null)
            {
               taskStartDate = task.getStart();
            }
         }

         if (taskStartDate != null)
         {
            if (startDate == null)
            {
               startDate = taskStartDate;
            }
            else
            {
               if (taskStartDate.getTime() < startDate.getTime())
               {
                  startDate = taskStartDate;
               }
            }
         }
      }

      return (startDate);
   }

   /**
    * Find the latest task finish date. We treat this as the
    * finish date for the project.
    *
    * @return finish date
    */
   public Date getFinishDate()
   {
      Date finishDate = null;

      for (Task task : m_allTasks)
      {
         //
         // If a hidden "summary" task is present we ignore it
         //
         if (NumberHelper.getInt(task.getUniqueID()) == 0)
         {
            continue;
         }

         //
         // Select the actual or forecast start date
         //
         Date taskFinishDate;
         taskFinishDate = task.getActualFinish();
         if (taskFinishDate == null)
         {
            taskFinishDate = task.getFinish();
         }

         if (taskFinishDate != null)
         {
            if (finishDate == null)
            {
               finishDate = taskFinishDate;
            }
            else
            {
               if (taskFinishDate.getTime() > finishDate.getTime())
               {
                  finishDate = taskFinishDate;
               }
            }
         }
      }

      return (finishDate);
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a task has been read from a project file.
    *
    * @param task task instance
    */
   public void fireTaskReadEvent(Task task)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.taskRead(task);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a task has been written to a project file.
    *
    * @param task task instance
    */
   public void fireTaskWrittenEvent(Task task)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.taskWritten(task);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a resource has been read from a project file.
    *
    * @param resource resource instance
    */
   public void fireResourceReadEvent(Resource resource)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.resourceRead(resource);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a resource has been written to a project file.
    *
    * @param resource resource instance
    */
   public void fireResourceWrittenEvent(Resource resource)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.resourceWritten(resource);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a calendar has been read from a project file.
    *
    * @param calendar calendar instance
    */
   public void fireCalendarReadEvent(ProjectCalendar calendar)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.calendarRead(calendar);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a resource assignment has been read from a project file.
    *
    * @param resourceAssignment resourceAssignment instance
    */
   public void fireAssignmentReadEvent(ResourceAssignment resourceAssignment)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.assignmentRead(resourceAssignment);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a resource assignment has been written to a project file.
    *
    * @param resourceAssignment resourceAssignment instance
    */
   public void fireAssignmentWrittenEvent(ResourceAssignment resourceAssignment)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.assignmentWritten(resourceAssignment);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a relation has been read from a project file.
    *
    * @param relation relation instance
    */
   public void fireRelationReadEvent(Relation relation)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.relationRead(relation);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a relation has been written to a project file.
    *
    * @param relation relation instance
    */
   public void fireRelationWrittenEvent(Relation relation)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.relationWritten(relation);
         }
      }
   }

   /**
    * This method is called to alert project listeners to the fact that
    * a calendar has been written to a project file.
    *
    * @param calendar calendar instance
    */
   public void fireCalendarWrittenEvent(ProjectCalendar calendar)
   {
      if (m_projectListeners != null)
      {
         for (ProjectListener listener : m_projectListeners)
         {
            listener.calendarWritten(calendar);
         }
      }
   }

   /**
    * Adds a listener to this project file.
    *
    * @param listener listener instance
    */
   public void addProjectListener(ProjectListener listener)
   {
      if (m_projectListeners == null)
      {
         m_projectListeners = new LinkedList<ProjectListener>();
      }
      m_projectListeners.add(listener);
   }

   /**
    * Adds a collection of listeners to the current project.
    * 
    * @param listeners collection of listeners
    */
   public void addProjectListeners(List<ProjectListener> listeners)
   {
      if (listeners != null)
      {
         for (ProjectListener listener : listeners)
         {
            addProjectListener(listener);
         }
      }
   }

   /**
    * Removes a listener from this project file.
    *
    * @param listener listener instance
    */
   public void removeProjectListener(ProjectListener listener)
   {
      if (m_projectListeners != null)
      {
         m_projectListeners.remove(listener);
      }
   }

   /**
    * Associates an alias with a custom task field number.
    *
    * @param field custom field number
    * @param alias alias text
    */
   public void setTaskFieldAlias(TaskField field, String alias)
   {
      if ((alias != null) && (alias.length() != 0))
      {
         m_taskFieldAlias.put(field, alias);
         m_aliasTaskField.put(alias, field);
      }
   }

   /**
    * Retrieves the alias associated with a custom task field.
    * This method will return null if no alias has been defined for
    * this field.
    *
    * @param field task field instance
    * @return alias text
    */
   public String getTaskFieldAlias(TaskField field)
   {
      return (m_taskFieldAlias.get(field));
   }

   /**
    * Retrieves a task field instance based on its alias. If the
    * alias is not recognised, this method will return null.
    *
    * @param alias alias text
    * @return task field instance
    */
   public TaskField getTaskFieldByAlias(String alias)
   {
      return (m_aliasTaskField.get(alias));
   }

   /**
    * Associates a value list with a custom task field number.
    *
    * @param field custom field number
    * @param values values for the value list
    */
   public void setTaskFieldValueList(TaskField field, List<Object> values)
   {
      if ((values != null) && (values.size() != 0))
      {
         m_taskFieldValueList.put(field, values);
      }
   }

   /**
    * Retrieves the value list associated with a custom task field.
    * This method will return null if no value list has been defined for
    * this field.
    *
    * @param field task field instance
    * @return alias text
    */
   public List<Object> getTaskFieldValueList(TaskField field)
   {
      return m_taskFieldValueList.get(field);
   }

   /**
    * Associates a descriptions for value list with a custom task field number.
    *
    * @param field custom field number
    * @param descriptions descriptions for the value list
    */
   public void setTaskFieldDescriptionList(TaskField field, List<String> descriptions)
   {
      if ((descriptions != null) && (descriptions.size() != 0))
      {
         m_taskFieldDescriptionList.put(field, descriptions);
      }
   }

   /**
    * Retrieves the description value list associated with a custom task field.
    * This method will return null if no descriptions for the value list has been defined for
    * this field.
    *
    * @param field task field instance
    * @return alias text
    */
   public List<String> getTaskFieldDescriptionList(TaskField field)
   {
      return m_taskFieldDescriptionList.get(field);
   }

   /**
    * Associates an alias with a custom resource field number.
    *
    * @param field custom field number
    * @param alias alias text
    */
   public void setResourceFieldAlias(ResourceField field, String alias)
   {
      if ((alias != null) && (alias.length() != 0))
      {
         m_resourceFieldAlias.put(field, alias);
         m_aliasResourceField.put(alias, field);
      }
   }

   /**
    * Retrieves the alias associated with a custom resource field.
    * This method will return null if no alias has been defined for
    * this field.
    *
    * @param field field number
    * @return alias text
    */
   public String getResourceFieldAlias(ResourceField field)
   {
      return (m_resourceFieldAlias.get(field));
   }

   /**
    * Retrieves a resource field based on its alias. If the
    * alias is not recognised, this method will return null.
    *
    * @param alias alias text
    * @return resource field instance
    */
   public ResourceField getResourceFieldByAlias(String alias)
   {
      return (m_aliasResourceField.get(alias));
   }

   /**
    * Allows derived classes to gain access to the mapping between
    * task fields and aliases.
    *
    * @return task field to alias map
    */
   public Map<TaskField, String> getTaskFieldAliasMap()
   {
      return (m_taskFieldAlias);
   }

   /**
    * Allows callers to gain access to the mapping between
    * resource field numbers and aliases.
    *
    * @return resource field to alias map
    */
   public Map<ResourceField, String> getResourceFieldAliasMap()
   {
      return (m_resourceFieldAlias);
   }

   /**
    * Removes an id-to-task mapping.
    *
    * @param id task unique ID
    */
   void unmapTaskUniqueID(Integer id)
   {
      m_taskUniqueIDMap.remove(id);
   }

   /**
    * Adds an id-to-task mapping.
    *
    * @param id task unique ID
    * @param task task instance
    */
   void mapTaskUniqueID(Integer id, Task task)
   {
      m_taskUniqueIDMap.put(id, task);
   }

   /**
    * Removes an id-to-task mapping.
    *
    * @param id task ID
    */
   void unmapTaskID(Integer id)
   {
      m_taskIDMap.remove(id);
   }

   /**
    * Adds an id-to-task mapping.
    *
    * @param id task ID
    * @param task task instance
    */
   void mapTaskID(Integer id, Task task)
   {
      m_taskIDMap.put(id, task);
   }

   /**
    * Removes an id-to-resource mapping.
    *
    * @param id resource unique ID
    */
   void unmapResourceUniqueID(Integer id)
   {
      m_resourceUniqueIDMap.remove(id);
   }

   /**
    * Adds an id-to-resource mapping.
    *
    * @param id resource unique ID
    * @param resource resource instance
    */
   void mapResourceUniqueID(Integer id, Resource resource)
   {
      m_resourceUniqueIDMap.put(id, resource);
   }

   /**
    * Removes an id-to-resource mapping.
    *
    * @param id resource ID
    */
   void unmapResourceID(Integer id)
   {
      m_resourceIDMap.remove(id);
   }

   /**
    * Adds an id-to-resource mapping.
    *
    * @param id resource ID
    * @param resource resource instance
    */
   void mapResourceID(Integer id, Resource resource)
   {
      m_resourceIDMap.put(id, resource);
   }

   /**
    * Removes an id-to-calendar mapping.
    *
    * @param id calendar unique ID
    */
   void unmapCalendarUniqueID(Integer id)
   {
      m_calendarUniqueIDMap.remove(id);
   }

   /**
    * Adds an id-to-calendar mapping.
    *
    * @param id calendar unique ID
    * @param calendar calendar instance
    */
   void mapCalendarUniqueID(Integer id, ProjectCalendar calendar)
   {
      m_calendarUniqueIDMap.put(id, calendar);
   }

   /**
    * Package-private method used to add views to this MPP file.
    *
    * @param view view data
    */
   public void addView(View view)
   {
      m_views.add(view);
   }

   /**
    * This method returns a list of the views defined in this MPP file.
    *
    * @return list of views
    */
   public List<View> getViews()
   {
      return (m_views);
   }

   /**
    * Package-private method used to add tables to this MPP file.
    *
    * @param table table data
    */
   public void addTable(Table table)
   {
      m_tables.add(table);
      if (table.getResourceFlag() == false)
      {
         m_taskTablesByName.put(table.getName(), table);
      }
      else
      {
         m_resourceTablesByName.put(table.getName(), table);
      }
   }

   /**
    * This method returns a list of the tables defined in this MPP file.
    *
    * @return list of tables
    */
   public List<Table> getTables()
   {
      return (m_tables);
   }

   /**
    * Adds a filter definition to this project file.
    * 
    * @param filter filter definition
    */
   public void addFilter(Filter filter)
   {
      if (filter.isTaskFilter())
      {
         m_taskFilters.add(filter);
      }

      if (filter.isResourceFilter())
      {
         m_resourceFilters.add(filter);
      }

      m_filtersByName.put(filter.getName(), filter);
      m_filtersByID.put(filter.getID(), filter);
   }

   /**
    * Removes a filter from this project file.
    *
    * @param filterName The name of the filter
    */
   public void removeFilter(String filterName)
   {
      Filter filter = getFilterByName(filterName);
      if (filter != null)
      {
         if (filter.isTaskFilter())
         {
            m_taskFilters.remove(filter);
         }

         if (filter.isResourceFilter())
         {
            m_resourceFilters.remove(filter);
         }
         m_filtersByName.remove(filterName);
         m_filtersByID.remove(filter.getID());
      }
   }

   /**
    * Retrieves a list of all resource filters.
    * 
    * @return list of all resource filters
    */
   public List<Filter> getAllResourceFilters()
   {
      return (m_resourceFilters);
   }

   /**
    * Retrieves a list of all task filters.
    * 
    * @return list of all task filters
    */
   public List<Filter> getAllTaskFilters()
   {
      return (m_taskFilters);
   }

   /**
    * Retrieve a given filter by name.
    * 
    * @param name filter name
    * @return filter instance
    */
   public Filter getFilterByName(String name)
   {
      return (m_filtersByName.get(name));
   }

   /**
    * Retrieve a given filter by ID.
    * 
    * @param id filter ID
    * @return filter instance
    */
   public Filter getFilterByID(Integer id)
   {
      return (m_filtersByID.get(id));
   }

   /**
    * Retrieves a list of all groups.
    * 
    * @return list of all groups
    */
   public List<Group> getAllGroups()
   {
      return (m_groups);
   }

   /**
    * Retrieve a given group by name.
    * 
    * @param name group name
    * @return Group instance
    */
   public Group getGroupByName(String name)
   {
      return (m_groupsByName.get(name));
   }

   /**
    * Adds a group definition to this project file.
    * 
    * @param group group definition
    */
   public void addGroup(Group group)
   {
      m_groups.add(group);
      m_groupsByName.put(group.getName(), group);
   }

   /**
    * Adds the definition of a graphical indicator for a field type.
    * 
    * @param field field type
    * @param indicator graphical indicator definition
    */
   public void addGraphicalIndicator(FieldType field, GraphicalIndicator indicator)
   {
      m_graphicalIndicators.put(field, indicator);
   }

   /**
    * Retrieves the definition of any graphical indicators used for the
    * given field type.
    * 
    * @param field field type
    * @return graphical indicator definition
    */
   public GraphicalIndicator getGraphicalIndicator(FieldType field)
   {
      return (m_graphicalIndicators.get(field));
   }

   /**
    * Utility method to retrieve the definition of a task table by name.
    * This method will return null if the table name is not recognised.
    *
    * @param name table name
    * @return table instance
    */
   public Table getTaskTableByName(String name)
   {
      return (m_taskTablesByName.get(name));
   }

   /**
    * Utility method to retrieve the definition of a resource table by name.
    * This method will return null if the table name is not recognised.
    *
    * @param name table name
    * @return table instance
    */
   public Table getResourceTableByName(String name)
   {
      return (m_resourceTablesByName.get(name));
   }

   /**
    * This package-private method is used to add resource sub project details.
    *
    * @param project sub project
    */
   public void setResourceSubProject(SubProject project)
   {
      m_resourceSubProject = project;
   }

   /**
    * Retrieves details of the sub project file used as a resource pool.
    *
    * @return sub project details
    */
   public SubProject getResourceSubProject()
   {
      return (m_resourceSubProject);
   }

   /**
    * This package-private method is used to add sub project details.
    *
    * @param project sub project
    */
   public void addSubProject(SubProject project)
   {
      m_allSubProjects.add(project);
   }

   /**
    * Retrieves all the subprojects for this MPX file.
    *
    * @return all sub project details
    */
   public List<SubProject> getAllSubProjects()
   {
      return (m_allSubProjects);
   }

   /**
    * Retrieve a flag indicating if auto filter is enabled.
    * 
    * @return auto filter flag
    */
   public boolean getAutoFilter()
   {
      return (m_autoFilter);
   }

   /**
    * Sets a flag indicating if auto filter is enabled.
    * 
    * @param autoFilter boolean flag
    */
   public void setAutoFilter(boolean autoFilter)
   {
      m_autoFilter = autoFilter;
   }

   /**
    * Set the saved view state associated with this file.
    * 
    * @param viewState view state
    */
   public void setViewState(ViewState viewState)
   {
      m_viewState = viewState;
   }

   /**
    * Retrieve the saved view state associated with this file.
    * 
    * @return view state
    */
   public ViewState getViewState()
   {
      return (m_viewState);
   }

   /**
    * Retrieves the default calendar for this project based on the calendar name
    * given in the project header. If a calendar of this name cannot be found, then
    * the first calendar listed for the project will be returned. If the
    * project contains no calendars, then a default calendar is added. 
    * 
    * @return default projectCalendar instance
    */
   public ProjectCalendar getCalendar()
   {
      String calendarName = m_projectHeader.getCalendarName();
      ProjectCalendar calendar = getCalendarByName(calendarName);
      if (calendar == null)
      {
         if (m_calendars.isEmpty())
         {
            calendar = addDefaultBaseCalendar();
         }
         else
         {
            calendar = m_calendars.get(0);
         }
      }
      return calendar;
   }

   /**
    * Sets the default calendar for this project.
    * 
    * @param calendar default calendar instance
    */
   public void setCalendar(ProjectCalendar calendar)
   {
      m_projectHeader.setCalendarName(calendar.getName());
   }

   /**
    * Retrieve the calendar used internally for timephased baseline calculation.
    * 
    * @return baseline calendar
    */
   public ProjectCalendar getBaselineCalendar()
   {
      //
      // Attempt to locate the calendar normally used by baselines
      // If this isn't present, fall back to using the default 
      // project calendar.
      //
      ProjectCalendar result = getCalendarByName("Used for Microsoft Project 98 Baseline Calendar");
      if (result == null)
      {
         result = getCalendar();
      }
      return result;
   }

   /**
    * Counter used to populate the unique ID field of a task.
    */
   private int m_taskUniqueID;

   /**
    * Counter used to populate the unique ID field of a calendar.
    */
   private int m_calendarUniqueID;

   /**
    * Counter used to populate the unique ID field of an assignment.
    */
   private int m_assignmentUniqueID;

   /**
    * Counter used to populate the ID field of a task.
    */
   private int m_taskID;

   /**
    * Counter used to populate the unique ID field of a resource.
    */
   private int m_resourceUniqueID;

   /**
    * Counter used to populate the ID field of a resource.
    */
   private int m_resourceID;

   /**
    * This list holds a reference to all resources defined in the
    * MPX file.
    */
   private List<Resource> m_allResources = new LinkedList<Resource>();

   /**
    * This list holds a reference to all tasks defined in the
    * MPX file.
    */
   private List<Task> m_allTasks = new LinkedList<Task>();

   /**
    * List holding references to the top level tasks
    * as defined by the outline level.
    */
   private List<Task> m_childTasks = new LinkedList<Task>();

   /**
    * This list holds a reference to all resource assignments defined in the
    * MPX file.
    */
   private List<ResourceAssignment> m_allResourceAssignments = new LinkedList<ResourceAssignment>();

   /**
    * List holding references to all calendars.
    */
   private List<ProjectCalendar> m_calendars = new LinkedList<ProjectCalendar>();

   /**
    * Project header record.
    */
   private ProjectHeader m_projectHeader = new ProjectHeader(this);

   /**
    * Indicating whether WBS value should be calculated on creation, or will
    * be manually set.
    */
   private boolean m_autoWBS = true;

   /**
    * Indicating whether the Outline Level value should be calculated on
    * creation, or will be manually set.
    */
   private boolean m_autoOutlineLevel = true;

   /**
    * Indicating whether the Outline Number value should be calculated on
    * creation, or will be manually set.
    */
   private boolean m_autoOutlineNumber = true;

   /**
    * Indicating whether the unique ID of a task should be
    * calculated on creation, or will be manually set.
    */
   private boolean m_autoTaskUniqueID = true;

   /**
    * Indicating whether the unique ID of a calendar should be
    * calculated on creation, or will be manually set.
    */
   private boolean m_autoCalendarUniqueID = true;

   /**
    * Indicating whether the unique ID of an assignment should be
    * calculated on creation, or will be manually set.
    */
   private boolean m_autoAssignmentUniqueID = true;

   /**
    * Indicating whether the ID of a task should be
    * calculated on creation, or will be manually set.
    */
   private boolean m_autoTaskID = true;

   /**
    * Indicating whether the unique ID of a resource should be
    * calculated on creation, or will be manually set.
    */
   private boolean m_autoResourceUniqueID = true;

   /**
    * Indicating whether the ID of a resource should be
    * calculated on creation, or will be manually set.
    */
   private boolean m_autoResourceID = true;

   /**
    * Maps from a task field number to a task alias.
    */
   private Map<TaskField, String> m_taskFieldAlias = new HashMap<TaskField, String>();

   /**
    * Maps from a task field number to a value list.
    */
   private Map<TaskField, List<Object>> m_taskFieldValueList = new HashMap<TaskField, List<Object>>();

   /**
    * Maps from a task field number to a description list.
    */
   private Map<TaskField, List<String>> m_taskFieldDescriptionList = new HashMap<TaskField, List<String>>();

   /**
    * Maps from a task field alias to a task field number.
    */
   private Map<String, TaskField> m_aliasTaskField = new HashMap<String, TaskField>();

   /**
    * Maps from a resource field number to a resource alias.
    */
   private Map<ResourceField, String> m_resourceFieldAlias = new HashMap<ResourceField, String>();

   /**
    * Maps from a resource field alias to a resource field number.
    */
   private Map<String, ResourceField> m_aliasResourceField = new HashMap<String, ResourceField>();

   /**
    * Maps from a task unique ID to a task instance.
    */
   private Map<Integer, Task> m_taskUniqueIDMap = new HashMap<Integer, Task>();

   /**
    * Maps from a task ID to a task instance.
    */
   private Map<Integer, Task> m_taskIDMap = new HashMap<Integer, Task>();

   /**
    * Maps from a resource unique ID to a resource instance.
    */
   private Map<Integer, Resource> m_resourceUniqueIDMap = new HashMap<Integer, Resource>();

   /**
    * Maps from a resource ID to a resource instance.
    */
   private Map<Integer, Resource> m_resourceIDMap = new HashMap<Integer, Resource>();

   /**
    * Maps from a calendar unique ID to a calendar instance.
    */
   private Map<Integer, ProjectCalendar> m_calendarUniqueIDMap = new HashMap<Integer, ProjectCalendar>();

   /**
    * List of project event listeners.
    */
   private List<ProjectListener> m_projectListeners;

   /**
    * List of views defined in this file.
    */
   private List<View> m_views = new ArrayList<View>();

   /**
    * List of tables defined in this file.
    */
   private List<Table> m_tables = new ArrayList<Table>();

   /**
    * Map of graphical indicator data.
    */
   private Map<FieldType, GraphicalIndicator> m_graphicalIndicators = new HashMap<FieldType, GraphicalIndicator>();

   /**
    * Index of task tables by name.
    */
   private Map<String, Table> m_taskTablesByName = new HashMap<String, Table>();

   /**
    * Index of resource tables by name.
    */
   private Map<String, Table> m_resourceTablesByName = new HashMap<String, Table>();

   /**
    * List of all task filters.
    */
   private List<Filter> m_taskFilters = new ArrayList<Filter>();

   /**
    * List of all resource filters.
    */
   private List<Filter> m_resourceFilters = new ArrayList<Filter>();

   /**
    * Index of filters by name.
    */
   private Map<String, Filter> m_filtersByName = new HashMap<String, Filter>();

   /**
    * Index of filters by ID.
    */
   private Map<Integer, Filter> m_filtersByID = new HashMap<Integer, Filter>();

   /**
    * List of all groups.
    */
   private List<Group> m_groups = new ArrayList<Group>();

   /**
    * Index of groups by name.
    */
   private Map<String, Group> m_groupsByName = new HashMap<String, Group>();

   /**
    * Resource sub project.
    */
   private SubProject m_resourceSubProject;

   /**
    * This list holds a reference to all subprojects defined in the
    * MPX file.
    */
   private List<SubProject> m_allSubProjects = new LinkedList<SubProject>();

   /**
    * Flag indicating if auto filter is enabled.
    */
   private boolean m_autoFilter;

   /**
    * Saved view state.
    */
   private ViewState m_viewState;

   /**
    * Maximum unique ID value MS Project will accept.
    */
   private static final int MS_PROJECT_MAX_UNIQUE_ID = 0x1FFFFF;
}
